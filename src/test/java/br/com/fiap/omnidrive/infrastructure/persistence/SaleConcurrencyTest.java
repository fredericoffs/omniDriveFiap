package br.com.fiap.omnidrive.infrastructure.persistence;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import br.com.fiap.omnidrive.TestcontainersConfiguration;
import br.com.fiap.omnidrive.application.gateway.PaymentGateway;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.usecase.sale.CancelSale;
import br.com.fiap.omnidrive.application.usecase.sale.CreateSale;
import br.com.fiap.omnidrive.application.usecase.sale.CreateSaleInput;
import br.com.fiap.omnidrive.application.usecase.sale.ProcessPaymentResult;
import br.com.fiap.omnidrive.application.usecase.sale.ProcessPaymentResultInput;
import br.com.fiap.omnidrive.application.usecase.sale.SaleWithPayment;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.PaymentResult;
import br.com.fiap.omnidrive.domain.sale.PaymentStatus;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de concorrência da venda e do pagamento com os gateways JPA e o {@link SpringTransactionRunner}, contra o
 * Postgres do Testcontainers. Cada operação roda numa virtual thread com a própria transação. Uma barreira segura a
 * leitura até as duas operações lerem o mesmo estado, então a corrida acontece em toda execução e a perdedora cai
 * sempre no lock otimista ({@link ConcurrentUpdateException}), sem deixar gravação parcial.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    TestcontainersConfiguration.class,
    JpaVehicleGateway.class,
    JpaSaleGateway.class,
    JpaPaymentGateway.class,
    SpringTransactionRunner.class
})
class SaleConcurrencyTest {

  private static final long TIMEOUT_SECONDS = 30;

  @Autowired
  private JpaVehicleGateway vehicleGateway;

  @Autowired
  private JpaSaleGateway saleGateway;

  @Autowired
  private JpaPaymentGateway paymentGateway;

  @Autowired
  private SpringTransactionRunner transactionRunner;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  private static Vehicle available(
      String chassis,
      String plate
  ) {
    var vehicle = Vehicle.register(
        new Chassis(chassis),
        new VehicleDetails(
            "Fiat",
            "Uno",
            2020,
            "Prata",
            new Plate(plate),
            45000,
            Money.of("30000"),
            Money.of("40000"),
            VehicleOrigin.OWNED
        )
    );
    vehicle.startPreparation();
    vehicle.releaseForSale();
    return vehicle;
  }

  private static CreateSaleInput saleInput(UUID vehicleId) {
    return new CreateSaleInput(
        vehicleId,
        "529.982.247-25",
        LocalDate.of(2026, 10, 1)
    );
  }

  private static List<Object> runConcurrently(
      Callable<Object> first,
      Callable<Object> second
  ) throws InterruptedException {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      return executor.invokeAll(
          List.of(first, second),
          TIMEOUT_SECONDS,
          SECONDS
      ).stream().map(SaleConcurrencyTest::outcome).toList();
    }
  }

  private static Object outcome(Future<Object> future) {
    return switch (future.state()) {
      case SUCCESS -> future.resultNow();
      case FAILED -> future.exceptionNow();
      case RUNNING, CANCELLED -> throw new IllegalStateException("Operação não terminou: " + future.state());
    };
  }

  @Test
  void onlyOneOfTwoSimultaneousSalesOfSameVehicleWins() throws InterruptedException {
    var vehicle = vehicleGateway.save(available("9BWZZZ377LT009501", "TXE9501"));
    var barrier = new CyclicBarrier(2);
    var barrierVehicleGateway = mock(VehicleGateway.class, delegatesTo(vehicleGateway));
    doAnswer(invocation -> {
      barrier.await(TIMEOUT_SECONDS, SECONDS);
      return vehicleGateway.findById(invocation.getArgument(0));
    }).when(barrierVehicleGateway).findById(any());
    var createSale = new CreateSale(barrierVehicleGateway, saleGateway, paymentGateway, transactionRunner);
    var input = saleInput(vehicle.getId());

    var outcomes = runConcurrently(
        () -> createSale.execute(input),
        () -> createSale.execute(input)
    );

    assertThat(outcomes)
        .<Class<?>>extracting(Object::getClass)
        .containsExactlyInAnyOrder(SaleWithPayment.class, ConcurrentUpdateException.class);
    var winner = outcomes.stream()
        .filter(SaleWithPayment.class::isInstance)
        .map(SaleWithPayment.class::cast)
        .findFirst()
        .orElseThrow();
    var reserved = vehicleGateway.findById(vehicle.getId()).orElseThrow();
    assertThat(reserved.getStatus()).isEqualTo(VehicleStatus.RESERVED);
    assertThat(reserved.getCurrentSaleId()).isEqualTo(winner.sale().getId());
    assertThat(count("select count(*) from sale where vehicle_id = ?", vehicle.getId())).isOne();
    assertThat(
        count(
            "select count(*) from payment p join sale s on s.id = p.sale_id where s.vehicle_id = ?",
            vehicle.getId()
        )
    ).isOne();
    assertThat(historyCount(vehicle.getId(), VehicleStatus.RESERVED)).isOne();
  }

  @Test
  void appliesOnlyOneOfTwoSimultaneousEqualWebhooks() throws InterruptedException {
    var vehicle = vehicleGateway.save(available("9BWZZZ377LT009502", "TXE9502"));
    var payment = sell(vehicle);
    var barrierPaymentGateway = barrierPaymentGateway(new CyclicBarrier(2));
    var processPaymentResult = new ProcessPaymentResult(
        barrierPaymentGateway,
        saleGateway,
        vehicleGateway,
        transactionRunner
    );
    var input = new ProcessPaymentResultInput(payment.getPaymentCode(), PaymentResult.PAID);

    var outcomes = runConcurrently(
        () -> processPaymentResult.execute(input),
        () -> processPaymentResult.execute(input)
    );

    assertThat(outcomes)
        .<Class<?>>extracting(Object::getClass)
        .containsExactlyInAnyOrder(Payment.class, ConcurrentUpdateException.class);
    var paid = paymentGateway.findBySaleId(payment.getSaleId()).orElseThrow();
    assertThat(paid.getStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(paid.getVersion()).isEqualTo(payment.getVersion() + 1);
    assertThat(vehicleGateway.findById(vehicle.getId()).orElseThrow().getStatus()).isEqualTo(VehicleStatus.SOLD);
    assertThat(historyCount(vehicle.getId(), VehicleStatus.SOLD)).isOne();

    var resent = new ProcessPaymentResult(paymentGateway, saleGateway, vehicleGateway, transactionRunner)
        .execute(input);

    assertThat(resent.getStatus()).isEqualTo(PaymentStatus.PAID);
    assertThat(paymentGateway.findBySaleId(payment.getSaleId()).orElseThrow().getVersion())
        .isEqualTo(paid.getVersion());
    assertThat(historyCount(vehicle.getId(), VehicleStatus.SOLD)).isOne();
  }

  @Test
  void keepsConsistentStateWhenStoreCancellationAndPaidWebhookAreSimultaneous() throws InterruptedException {
    var vehicle = vehicleGateway.save(available("9BWZZZ377LT009503", "TXE9503"));
    var payment = sell(vehicle);
    var barrierPaymentGateway = barrierPaymentGateway(new CyclicBarrier(2));
    var cancelSale = new CancelSale(
        saleGateway,
        barrierPaymentGateway,
        vehicleGateway,
        transactionRunner
    );
    var processPaymentResult = new ProcessPaymentResult(
        barrierPaymentGateway,
        saleGateway,
        vehicleGateway,
        transactionRunner
    );
    var input = new ProcessPaymentResultInput(payment.getPaymentCode(), PaymentResult.PAID);
    var historyBefore = historyCount(vehicle.getId());

    var outcomes = runConcurrently(
        () -> cancelSale.execute(payment.getSaleId()),
        () -> processPaymentResult.execute(input)
    );

    assertThat(outcomes)
        .filteredOn(ConcurrentUpdateException.class::isInstance)
        .hasSize(1);
    var finalPayment = paymentGateway.findBySaleId(payment.getSaleId()).orElseThrow();
    var finalVehicle = vehicleGateway.findById(vehicle.getId()).orElseThrow();
    assertThat(tuple(finalPayment.getStatus(), finalVehicle.getStatus()))
        .isIn(
            tuple(PaymentStatus.PAID, VehicleStatus.SOLD),
            tuple(PaymentStatus.CANCELED, VehicleStatus.AVAILABLE)
        );
    assertThat(finalPayment.getVersion()).isEqualTo(payment.getVersion() + 1);
    assertThat(historyCount(vehicle.getId())).isEqualTo(historyBefore + 1);
  }

  private PaymentGateway barrierPaymentGateway(CyclicBarrier barrier) {
    var barrierPaymentGateway = mock(PaymentGateway.class, delegatesTo(paymentGateway));
    doAnswer(invocation -> {
      barrier.await(TIMEOUT_SECONDS, SECONDS);
      return paymentGateway.findByPaymentCode(invocation.getArgument(0));
    }).when(barrierPaymentGateway).findByPaymentCode(any());
    doAnswer(invocation -> {
      barrier.await(TIMEOUT_SECONDS, SECONDS);
      return paymentGateway.findBySaleId(invocation.getArgument(0));
    }).when(barrierPaymentGateway).findBySaleId(any());
    return barrierPaymentGateway;
  }

  private Payment sell(Vehicle vehicle) {
    var createSale = new CreateSale(vehicleGateway, saleGateway, paymentGateway, transactionRunner);
    return createSale.execute(saleInput(vehicle.getId())).payment();
  }

  private int historyCount(UUID vehicleId) {
    return count("select count(*) from vehicle_status_history where vehicle_id = ?", vehicleId);
  }

  private int historyCount(
      UUID vehicleId,
      VehicleStatus newStatus
  ) {
    return count(
        "select count(*) from vehicle_status_history where vehicle_id = ? and new_status = ?",
        vehicleId,
        newStatus.name()
    );
  }

  private int count(
      String sql,
      Object... args
  ) {
    return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Integer.class, args)).orElse(0);
  }
}

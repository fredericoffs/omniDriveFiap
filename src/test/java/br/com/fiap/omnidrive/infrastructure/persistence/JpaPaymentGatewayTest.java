package br.com.fiap.omnidrive.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.omnidrive.TestcontainersConfiguration;
import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.PaymentResult;
import br.com.fiap.omnidrive.domain.sale.PaymentStatus;
import br.com.fiap.omnidrive.domain.sale.Sale;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Testes de integração do {@link JpaPaymentGateway} contra o Postgres do Testcontainers, com o schema das migrations
 * do Flyway.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaVehicleGateway.class, JpaSaleGateway.class, JpaPaymentGateway.class})
class JpaPaymentGatewayTest {

  @Autowired
  private JpaPaymentGateway gateway;

  @Autowired
  private JpaVehicleGateway vehicleGateway;

  @Autowired
  private JpaSaleGateway saleGateway;

  @Autowired
  private TestEntityManager entityManager;

  private UUID saleId;

  @BeforeEach
  void setUp() {
    var vehicle = Vehicle.register(
        new Chassis("9BWZZZ377LT004251"),
        new VehicleDetails(
            "Fiat",
            "Uno",
            2020,
            "Prata",
            new Plate("ABC1D23"),
            45000,
            Money.of("30000"),
            Money.of("40000"),
            VehicleOrigin.OWNED
        )
    );
    vehicle.startPreparation();
    vehicle.releaseForSale();
    var sale = Sale.create(
        vehicleGateway.save(vehicle),
        new Cpf("52998224725"),
        LocalDate.of(2026, 10, 1)
    );
    saleId = saleGateway.save(sale).getId();
  }

  @Test
  void savesNewPaymentAwaitingPayment() {
    var payment = Payment.open(saleId);

    var saved = gateway.save(payment);
    flushAndClear();

    assertThat(saved.getVersion()).isZero();
    var found = find(payment);
    assertThat(found.getPaymentCode()).isEqualTo(payment.getPaymentCode());
    assertThat(found.getSaleId()).isEqualTo(saleId);
    assertThat(found.getStatus()).isEqualTo(PaymentStatus.AWAITING_PAYMENT);
  }

  @Test
  void updatesStatusAndIncrementsVersion() {
    var saved = gateway.save(Payment.open(saleId));
    flushAndClear();

    saved.register(PaymentResult.PAID);
    var updated = gateway.save(saved);
    flushAndClear();

    assertThat(updated.getVersion()).isEqualTo(1);
    assertThat(find(saved).getStatus()).isEqualTo(PaymentStatus.PAID);
  }

  @Test
  void rejectsSaveBasedOnOutdatedVersion() {
    var payment = Payment.open(saleId);
    gateway.save(payment);
    flushAndClear();
    var firstRead = find(payment);
    var secondRead = find(payment);

    firstRead.register(PaymentResult.PAID);
    gateway.save(firstRead);
    secondRead.register(PaymentResult.DECLINED);

    assertThatThrownBy(() -> gateway.save(secondRead))
        .isInstanceOf(ConcurrentUpdateException.class)
        .hasMessageContaining(payment.getPaymentCode());
    flushAndClear();
    assertThat(find(payment).getStatus()).isEqualTo(PaymentStatus.PAID);
  }

  @Test
  void findsPaymentByCode() {
    var payment = Payment.open(saleId);
    gateway.save(payment);
    flushAndClear();

    var found = gateway.findByPaymentCode(payment.getPaymentCode());

    assertThat(found)
        .get()
        .satisfies(paymentFound -> {
          assertThat(paymentFound.getId()).isEqualTo(payment.getId());
          assertThat(paymentFound.getSaleId()).isEqualTo(saleId);
          assertThat(paymentFound.getStatus()).isEqualTo(PaymentStatus.AWAITING_PAYMENT);
          assertThat(paymentFound.getVersion()).isZero();
        });
  }

  @Test
  void returnsEmptyWhenPaymentCodeDoesNotExist() {
    assertThat(gateway.findByPaymentCode("unknown-code")).isEmpty();
  }

  @Test
  void findsPaymentBySaleId() {
    var payment = Payment.open(saleId);
    gateway.save(payment);
    flushAndClear();

    var found = gateway.findBySaleId(saleId);

    assertThat(found)
        .get()
        .satisfies(paymentFound -> {
          assertThat(paymentFound.getId()).isEqualTo(payment.getId());
          assertThat(paymentFound.getPaymentCode()).isEqualTo(payment.getPaymentCode());
          assertThat(paymentFound.getStatus()).isEqualTo(PaymentStatus.AWAITING_PAYMENT);
        });
  }

  @Test
  void returnsEmptyWhenSaleHasNoPayment() {
    assertThat(gateway.findBySaleId(UUID.randomUUID())).isEmpty();
  }

  private Payment find(Payment payment) {
    var found = Objects.requireNonNull(entityManager.find(PaymentEntity.class, payment.getId())).toDomain();
    entityManager.clear();
    return found;
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }
}

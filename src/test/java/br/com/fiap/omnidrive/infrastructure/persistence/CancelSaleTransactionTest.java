package br.com.fiap.omnidrive.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.TestcontainersConfiguration;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.usecase.sale.CancelSale;
import br.com.fiap.omnidrive.application.usecase.sale.CreateSale;
import br.com.fiap.omnidrive.application.usecase.sale.CreateSaleInput;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.PaymentStatus;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de integração do caso de uso {@link CancelSale} com os gateways JPA e o {@link SpringTransactionRunner},
 * contra o Postgres do Testcontainers. Os testes rodam sem a transação que o {@code @DataJpaTest} abre por padrão, para
 * que o commit e o rollback sejam os do próprio caso de uso.
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
class CancelSaleTransactionTest {

  @Autowired
  private JpaVehicleGateway vehicleGateway;

  @Autowired
  private JpaSaleGateway saleGateway;

  @Autowired
  private JpaPaymentGateway paymentGateway;

  @Autowired
  private SpringTransactionRunner transactionRunner;

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

  @Test
  void commitsPaymentAndVehicleTogether() {
    var vehicle = vehicleGateway.save(available("9BWZZZ377LT009301", "TXC9301"));
    var payment = sell(vehicle);
    var cancelSale = new CancelSale(saleGateway, paymentGateway, vehicleGateway, transactionRunner);

    cancelSale.execute(payment.getSaleId());

    assertThat(paymentGateway.findBySaleId(payment.getSaleId()).orElseThrow().getStatus())
        .isEqualTo(PaymentStatus.CANCELED);
    var available = vehicleGateway.findById(vehicle.getId()).orElseThrow();
    assertThat(available.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    assertThat(available.getCurrentSaleId()).isNull();
  }

  @Test
  void rollsBackPaymentWhenVehicleSaveFails() {
    var vehicle = vehicleGateway.save(available("9BWZZZ377LT009302", "TXC9302"));
    var payment = sell(vehicle);
    var reserved = vehicleGateway.findById(vehicle.getId()).orElseThrow();
    var failingVehicleGateway = mock(VehicleGateway.class);
    when(failingVehicleGateway.findById(vehicle.getId())).thenReturn(Optional.of(reserved));
    when(failingVehicleGateway.save(any())).thenThrow(new ConcurrentUpdateException("Falha forçada"));
    var cancelSale = new CancelSale(
        saleGateway,
        paymentGateway,
        failingVehicleGateway,
        transactionRunner
    );
    var saleId = payment.getSaleId();

    assertThatThrownBy(() -> cancelSale.execute(saleId))
        .isInstanceOf(ConcurrentUpdateException.class)
        .hasMessage("Falha forçada");
    assertThat(paymentGateway.findBySaleId(saleId).orElseThrow().getStatus())
        .isEqualTo(PaymentStatus.AWAITING_PAYMENT);
    assertThat(vehicleGateway.findById(vehicle.getId()).orElseThrow().getStatus())
        .isEqualTo(VehicleStatus.RESERVED);
  }

  private Payment sell(Vehicle vehicle) {
    var createSale = new CreateSale(vehicleGateway, saleGateway, paymentGateway, transactionRunner);
    return createSale.execute(
        new CreateSaleInput(
            vehicle.getId(),
            "529.982.247-25",
            LocalDate.of(2026, 10, 1)
        )
    ).payment();
  }
}

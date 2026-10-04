package br.com.fiap.omnidrive.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.TestcontainersConfiguration;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.usecase.CreateSale;
import br.com.fiap.omnidrive.application.usecase.CreateSaleInput;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de integração do caso de uso {@link CreateSale} com os gateways JPA e o {@link SpringTransactionRunner}, contra o Postgres do
 * Testcontainers. Os testes rodam sem a transação que o {@code @DataJpaTest} abre por padrão, para que o commit e o rollback sejam os do próprio caso
 * de uso.
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
class CreateSaleTransactionTest {

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

  private static CreateSaleInput input(UUID vehicleId) {
    return new CreateSaleInput(
        vehicleId,
        "529.982.247-25",
        LocalDate.of(2026, 10, 1)
    );
  }

  @Test
  void commitsSalePaymentReservationAndHistoryTogether() {
    var vehicle = vehicleGateway.save(available("9BWZZZ377LT009101", "TXB9101"));
    var createSale = new CreateSale(vehicleGateway, saleGateway, paymentGateway, transactionRunner);

    var result = createSale.execute(input(vehicle.getId()));

    var reserved = vehicleGateway.findById(vehicle.getId()).orElseThrow();
    assertThat(reserved.getStatus()).isEqualTo(VehicleStatus.RESERVED);
    assertThat(reserved.getCurrentSaleId()).isEqualTo(result.sale().getId());
    assertThat(countSales(vehicle.getId())).isOne();
    assertThat(countPayments(vehicle.getId())).isOne();
    assertThat(
        jdbcTemplate.queryForObject(
            "select count(*) from vehicle_status_history where vehicle_id = ? and sale_id = ? and new_status = 'RESERVED'",
            Integer.class,
            vehicle.getId(),
            result.sale().getId()
        )
    ).isOne();
  }

  @Test
  void rollsBackSaleAndPaymentWhenVehicleSaveFails() {
    var vehicle = vehicleGateway.save(available("9BWZZZ377LT009102", "TXB9102"));
    var failingVehicleGateway = mock(VehicleGateway.class);
    when(failingVehicleGateway.findById(vehicle.getId())).thenReturn(Optional.of(vehicle));
    when(failingVehicleGateway.save(any())).thenThrow(new ConcurrentUpdateException("Falha forçada"));
    var createSale = new CreateSale(failingVehicleGateway, saleGateway, paymentGateway, transactionRunner);
    var saleInput = input(vehicle.getId());

    assertThatThrownBy(() -> createSale.execute(saleInput))
        .isInstanceOf(ConcurrentUpdateException.class)
        .hasMessage("Falha forçada");
    assertThat(countSales(vehicle.getId())).isZero();
    assertThat(countPayments(vehicle.getId())).isZero();
    assertThat(vehicleGateway.findById(vehicle.getId()).orElseThrow().getStatus())
        .isEqualTo(VehicleStatus.AVAILABLE);
  }

  private int countSales(UUID vehicleId) {
    return Optional.ofNullable(
        jdbcTemplate.queryForObject("select count(*) from sale where vehicle_id = ?", Integer.class, vehicleId)
    ).orElse(0);
  }

  private int countPayments(UUID vehicleId) {
    return Optional.ofNullable(jdbcTemplate.queryForObject(
        "select count(*) from payment p join sale s on s.id = p.sale_id where s.vehicle_id = ?",
        Integer.class,
        vehicleId
    )).orElse(0);
  }
}

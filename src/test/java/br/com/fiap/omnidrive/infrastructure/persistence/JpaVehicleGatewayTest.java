package br.com.fiap.omnidrive.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.omnidrive.TestcontainersConfiguration;
import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.AlreadyExistsException;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.sale.Sale;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Testes de integração do {@link JpaVehicleGateway} contra o Postgres do Testcontainers, com o schema das
 * migrations do Flyway.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaVehicleGateway.class, JpaSaleGateway.class})
class JpaVehicleGatewayTest {

  private static final AtomicInteger SEQUENCE = new AtomicInteger(1000);

  @Autowired
  private JpaVehicleGateway gateway;

  @Autowired
  private JpaSaleGateway saleGateway;

  @Autowired
  private TestEntityManager entityManager;

  private static Vehicle register(String salePrice) {
    var sequence = SEQUENCE.incrementAndGet();
    return register(
        salePrice,
        "9BWZZZ377LT00" + sequence,
        "ABC" + sequence
    );
  }

  private static Vehicle register(
      String salePrice,
      String chassis,
      String plate
  ) {
    return Vehicle.register(
        new Chassis(chassis),
        new VehicleDetails(
            "Fiat",
            "Uno",
            2020,
            "Prata",
            new Plate(plate),
            45000,
            Money.of("30000"),
            Money.of(salePrice),
            VehicleOrigin.OWNED
        )
    );
  }

  private static VehicleDetails newDetails() {
    return new VehicleDetails(
        "Volkswagen",
        "Gol",
        2020,
        "Branco",
        new Plate("BRA2E19"),
        46000,
        Money.of("25000"),
        Money.of("32000"),
        VehicleOrigin.CONSIGNED
    );
  }

  private static Vehicle available(String salePrice) {
    var vehicle = register(salePrice);
    vehicle.startPreparation();
    vehicle.releaseForSale();
    return vehicle;
  }

  @Test
  void savesNewVehicleWithItsStatusHistory() {
    var vehicle = register("40000.50");

    gateway.save(vehicle);
    flushAndClear();

    var found = gateway.findById(vehicle.getId()).orElseThrow();
    assertThat(found.getChassis()).isEqualTo(vehicle.getChassis());
    assertThat(found.getPlate()).isEqualTo(vehicle.getPlate());
    assertThat(found.getMileage()).isEqualTo(45000);
    assertThat(found.getBrand()).isEqualTo("Fiat");
    assertThat(found.getModel()).isEqualTo("Uno");
    assertThat(found.getYear()).isEqualTo(2020);
    assertThat(found.getColor()).isEqualTo("Prata");
    assertThat(found.getPurchasePrice()).isEqualTo(Money.of("30000"));
    assertThat(found.getSalePrice()).isEqualTo(Money.of("40000.50"));
    assertThat(found.getOrigin()).isEqualTo(VehicleOrigin.OWNED);
    assertThat(found.getStatus()).isEqualTo(VehicleStatus.RECEIVED);
    assertThat(found.getStatusChanges()).isEmpty();
    assertThat(statusHistory(vehicle.getId())).containsExactly(VehicleStatus.RECEIVED);
  }

  @Test
  void updatesExistingVehicleKeepingCreationDateAndAppendingHistory() {
    var vehicle = register("40000");
    gateway.save(vehicle);
    flushAndClear();

    var found = gateway.findById(vehicle.getId()).orElseThrow();
    found.update(newDetails());
    found.startPreparation();
    gateway.save(found);
    flushAndClear();

    var updated = gateway.findById(vehicle.getId()).orElseThrow();
    assertThat(updated.getBrand()).isEqualTo("Volkswagen");
    assertThat(updated.getPlate()).isEqualTo(new Plate("BRA2E19"));
    assertThat(updated.getMileage()).isEqualTo(46000);
    assertThat(updated.getSalePrice()).isEqualTo(Money.of("32000"));
    assertThat(updated.getOrigin()).isEqualTo(VehicleOrigin.CONSIGNED);
    assertThat(updated.getStatus()).isEqualTo(VehicleStatus.IN_PREPARATION);
    assertThat(statusHistory(vehicle.getId()))
        .containsExactlyInAnyOrder(VehicleStatus.RECEIVED, VehicleStatus.IN_PREPARATION);
    assertThat(creationDateIsKept(vehicle.getId())).isTrue();
  }

  @Test
  void findsOnlyVehiclesWithStatusOrderedBySalePrice() {
    var expensive = available("50000");
    var cheap = available("20000");
    var middle = available("35000");
    var notAvailable = register("10000");
    List.of(expensive, cheap, middle, notAvailable).forEach(gateway::save);
    flushAndClear();

    var vehicles = gateway.findByStatusOrderBySalePrice(VehicleStatus.AVAILABLE);

    assertThat(vehicles)
        .extracting(Vehicle::getId)
        .containsExactly(cheap.getId(), middle.getId(), expensive.getId());
  }

  @Test
  void incrementsVersionOnEachChange() {
    var saved = gateway.save(register("40000"));
    assertThat(saved.getVersion()).isZero();
    assertThat(saved.getStatusChanges()).isEmpty();

    saved.startPreparation();
    var updated = gateway.save(saved);

    assertThat(updated.getVersion()).isEqualTo(1);
  }

  @Test
  void rejectsSaveBasedOnOutdatedVersion() {
    var vehicle = register("40000");
    gateway.save(vehicle);
    flushAndClear();
    var firstRead = gateway.findById(vehicle.getId()).orElseThrow();
    var secondRead = gateway.findById(vehicle.getId()).orElseThrow();

    firstRead.startPreparation();
    gateway.save(firstRead);
    secondRead.update(newDetails());

    assertThatThrownBy(() -> gateway.save(secondRead))
        .isInstanceOf(ConcurrentUpdateException.class)
        .hasMessageContaining(vehicle.getId().toString());
    flushAndClear();
    var current = gateway.findById(vehicle.getId()).orElseThrow();
    assertThat(current.getStatus()).isEqualTo(VehicleStatus.IN_PREPARATION);
    assertThat(current.getBrand()).isEqualTo("Fiat");
  }

  @Test
  void savesReadmissionOfSoldVehicle() {
    var vehicle = gateway.save(available("40000"));
    var saleId = saleGateway.save(
        Sale.create(
            vehicle,
            new Cpf("52998224725"),
            LocalDate.of(2026, 10, 1)
        )
    ).getId();
    vehicle.reserve(saleId);
    vehicle.confirmSale(saleId);
    gateway.save(vehicle);
    flushAndClear();

    var sold = gateway.findById(vehicle.getId()).orElseThrow();
    assertThat(sold.getCurrentSaleId()).isEqualTo(saleId);
    sold.readmit(Money.of("35000"), 52000);
    gateway.save(sold);
    flushAndClear();

    var readmitted = gateway.findById(vehicle.getId()).orElseThrow();
    assertThat(readmitted.getStatus()).isEqualTo(VehicleStatus.RECEIVED);
    assertThat(readmitted.getPurchasePrice()).isEqualTo(Money.of("35000"));
    assertThat(readmitted.getMileage()).isEqualTo(52000);
    assertThat(readmitted.getCurrentSaleId()).isNull();
    assertThat(statusHistory(vehicle.getId()))
        .containsExactlyInAnyOrder(
            VehicleStatus.RECEIVED,
            VehicleStatus.IN_PREPARATION,
            VehicleStatus.AVAILABLE,
            VehicleStatus.RESERVED,
            VehicleStatus.SOLD,
            VehicleStatus.RECEIVED
        );
  }

  @Test
  void rejectsCurrentSaleThatDoesNotExist() {
    var vehicle = gateway.save(available("40000"));
    vehicle.reserve(UUID.randomUUID());

    assertThatThrownBy(() -> gateway.save(vehicle))
        .isInstanceOf(DataIntegrityViolationException.class)
        .hasMessageContaining("fk_vehicle_current_sale");
  }

  @Test
  void checksExistenceByChassisAndPlate() {
    gateway.save(register("40000", "9BWZZZ377LT004251", "ABC1D23"));
    flushAndClear();

    assertThat(gateway.existsByChassis(new Chassis("9BWZZZ377LT004251"))).isTrue();
    assertThat(gateway.existsByChassis(new Chassis("9BWZZZ377LT004252"))).isFalse();
    assertThat(gateway.existsByPlate(new Plate("ABC1D23"))).isTrue();
    assertThat(gateway.existsByPlate(new Plate("ABC1D24"))).isFalse();
  }

  @Test
  void rejectsDuplicateChassisOnSave() {
    gateway.save(register("40000", "9BWZZZ377LT004251", "ABC1D23"));
    var duplicate = register("40000", "9BWZZZ377LT004251", "BRA2E19");

    assertThatThrownBy(() -> gateway.save(duplicate))
        .isInstanceOf(AlreadyExistsException.class)
        .hasMessage("Já existe um veículo com o chassi 9BWZZZ377LT004251");
  }

  @Test
  void rejectsDuplicatePlateOnSave() {
    gateway.save(register("40000", "9BWZZZ377LT004251", "ABC1D23"));
    var duplicate = register("40000", "9BWZZZ377LT004252", "ABC1D23");

    assertThatThrownBy(() -> gateway.save(duplicate))
        .isInstanceOf(AlreadyExistsException.class)
        .hasMessage("Já existe um veículo com a placa ABC1D23");
  }

  @Test
  void returnsEmptyWhenVehicleDoesNotExist() {
    assertThat(gateway.findById(UUID.randomUUID())).isEmpty();
  }

  private void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  private List<VehicleStatus> statusHistory(UUID vehicleId) {
    return entityManager.getEntityManager()
        .createQuery(
            "select h.newStatus from VehicleStatusHistoryEntity h where h.vehicleId = :vehicleId",
            VehicleStatus.class
        )
        .setParameter("vehicleId", vehicleId)
        .getResultList();
  }

  private boolean creationDateIsKept(UUID vehicleId) {
    var count = (Number) entityManager.getEntityManager()
        .createNativeQuery(
            "select count(*) from vehicle where id = :id and created_at is not null and updated_at >= created_at"
        )
        .setParameter("id", vehicleId)
        .getSingleResult();
    return count.intValue() == 1;
  }
}

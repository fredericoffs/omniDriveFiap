package br.com.fiap.omnidrive.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.omnidrive.TestcontainersConfiguration;
import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.sale.Sale;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import java.time.LocalDate;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * Testes de integração do {@link JpaSaleGateway} contra o Postgres do Testcontainers, com o schema das migrations do
 * Flyway.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaVehicleGateway.class, JpaSaleGateway.class})
class JpaSaleGatewayTest {

  @Autowired
  private JpaSaleGateway gateway;

  @Autowired
  private JpaVehicleGateway vehicleGateway;

  @Autowired
  private TestEntityManager entityManager;

  private static Vehicle available() {
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
            Money.of("40000.50"),
            VehicleOrigin.OWNED
        )
    );
    vehicle.startPreparation();
    vehicle.releaseForSale();
    return vehicle;
  }

  @Test
  void savesSaleWithAllItsData() {
    var vehicle = vehicleGateway.save(available());
    var sale = Sale.create(
        vehicle,
        new Cpf("529.982.247-25"),
        LocalDate.of(2026, 10, 1)
    );

    var saved = gateway.save(sale);
    entityManager.flush();
    entityManager.clear();

    assertThat(saved.getId()).isEqualTo(sale.getId());
    var found = Objects.requireNonNull(entityManager.find(SaleEntity.class, sale.getId())).toDomain();
    assertThat(found.getVehicleId()).isEqualTo(vehicle.getId());
    assertThat(found.getBuyerCpf()).isEqualTo(new Cpf("52998224725"));
    assertThat(found.getSaleDate()).isEqualTo(LocalDate.of(2026, 10, 1));
    assertThat(found.getAmount()).isEqualTo(Money.of("40000.50"));
  }
}

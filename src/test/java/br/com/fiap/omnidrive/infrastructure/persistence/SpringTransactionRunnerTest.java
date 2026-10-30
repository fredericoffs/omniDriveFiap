package br.com.fiap.omnidrive.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.omnidrive.TestcontainersConfiguration;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testes de integração do {@link SpringTransactionRunner} contra o Postgres do Testcontainers. Os testes rodam sem a
 * transação que o {@code @DataJpaTest} abre por padrão, para que o commit e o rollback sejam os do próprio runner.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({TestcontainersConfiguration.class, JpaVehicleGateway.class, SpringTransactionRunner.class})
class SpringTransactionRunnerTest {

  @Autowired
  private SpringTransactionRunner transactionRunner;

  @Autowired
  private JpaVehicleGateway gateway;

  private static Vehicle register(
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
            Money.of("40000"),
            VehicleOrigin.OWNED
        )
    );
  }

  @Test
  void commitsWhenWorkSucceeds() {
    var vehicle = register("9BWZZZ377LT009001", "TXA9001");

    var saved = transactionRunner.inTransaction(() -> gateway.save(vehicle));

    assertThat(saved.getVersion()).isZero();
    assertThat(gateway.findById(vehicle.getId())).isPresent();
  }

  @Test
  void rollsBackEverySaveWhenWorkFails() {
    var first = register("9BWZZZ377LT009002", "TXA9002");
    var second = register("9BWZZZ377LT009003", "TXA9003");
    Supplier<Vehicle> work = () -> {
      gateway.save(first);
      gateway.save(second);
      throw new BusinessRuleException("Falha depois de gravar");
    };

    assertThatThrownBy(() -> transactionRunner.inTransaction(work))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Falha depois de gravar");
    assertThat(gateway.findById(first.getId())).isEmpty();
    assertThat(gateway.findById(second.getId())).isEmpty();
  }
}

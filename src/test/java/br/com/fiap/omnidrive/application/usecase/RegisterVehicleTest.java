package br.com.fiap.omnidrive.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link RegisterVehicle}.
 */
@ExtendWith(MockitoExtension.class)
class RegisterVehicleTest {

  @Mock
  private VehicleGateway vehicleGateway;

  private RegisterVehicle registerVehicle;

  private static RegisterVehicleInput input(BigDecimal purchasePrice, BigDecimal salePrice) {
    return new RegisterVehicleInput(
        "Fiat",
        "Uno",
        2020,
        "Prata",
        purchasePrice,
        salePrice,
        VehicleOrigin.CONSIGNED
    );
  }

  @BeforeEach
  void setUp() {
    registerVehicle = new RegisterVehicle(vehicleGateway);
  }

  @Test
  void registersVehicleAsReceivedAndSavesIt() {
    var vehicle = registerVehicle.execute(input(new BigDecimal("30000"), new BigDecimal("40000.5")));

    assertThat(vehicle.getId()).isNotNull();
    assertThat(vehicle.getBrand()).isEqualTo("Fiat");
    assertThat(vehicle.getModel()).isEqualTo("Uno");
    assertThat(vehicle.getYear()).isEqualTo(2020);
    assertThat(vehicle.getColor()).isEqualTo("Prata");
    assertThat(vehicle.getPurchasePrice()).isEqualTo(Money.of("30000"));
    assertThat(vehicle.getSalePrice()).isEqualTo(Money.of("40000.50"));
    assertThat(vehicle.getOrigin()).isEqualTo(VehicleOrigin.CONSIGNED);
    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RECEIVED);
    assertThat(vehicle.getStatusChanges()).hasSize(1);
    verify(vehicleGateway).save(vehicle);
  }

  @Test
  void reportsWhichPriceIsMissing() {
    assertThatThrownBy(() -> registerVehicle.execute(input(null, new BigDecimal("40000"))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O valor de entrada é obrigatório");
    assertThatThrownBy(() -> registerVehicle.execute(input(new BigDecimal("30000"), null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O preço de venda é obrigatório");
  }

  @Test
  void doesNotSaveInvalidVehicle() {
    assertThatThrownBy(() -> registerVehicle.execute(input(new BigDecimal("30000"), BigDecimal.ZERO)))
        .isInstanceOf(BusinessRuleException.class);

    verify(vehicleGateway, never()).save(any());
  }
}

package br.com.fiap.omnidrive.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.AlreadyExistsException;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
        " 9bwzzz377lt004251 ",
        "abc-1234",
        45000,
        "Fiat",
        "Uno",
        2020,
        "Prata",
        purchasePrice,
        salePrice,
        VehicleOrigin.CONSIGNED
    );
  }

  private static Stream<Arguments> missingPriceArguments() {
    return Stream.of(
        Arguments.of(null, new BigDecimal("40000"), "O valor de entrada é obrigatório"),
        Arguments.of(new BigDecimal("30000"), null, "O preço de venda é obrigatório")
    );
  }

  @BeforeEach
  void setUp() {
    registerVehicle = new RegisterVehicle(vehicleGateway);
  }

  @Test
  void registersVehicleAsReceivedAndSavesIt() {
    when(vehicleGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var vehicle = registerVehicle.execute(input(new BigDecimal("30000"), new BigDecimal("40000.5")));

    assertThat(vehicle.getId()).isNotNull();
    assertThat(vehicle.getChassis()).isEqualTo(new Chassis("9BWZZZ377LT004251"));
    assertThat(vehicle.getPlate()).isEqualTo(new Plate("ABC1234"));
    assertThat(vehicle.getMileage()).isEqualTo(45000);
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

  @ParameterizedTest
  @MethodSource("missingPriceArguments")
  void reportsWhichPriceIsMissing(
      BigDecimal purchasePrice,
      BigDecimal salePrice,
      String expectedMessage
  ) {
    var registerVehicleInput = input(purchasePrice, salePrice);

    assertThatThrownBy(() -> registerVehicle.execute(registerVehicleInput))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(expectedMessage);

    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void rejectsDuplicateChassis() {
    when(vehicleGateway.existsByChassis(new Chassis("9BWZZZ377LT004251"))).thenReturn(true);
    var registerVehicleInput = input(new BigDecimal("30000"), new BigDecimal("40000"));

    assertThatThrownBy(() -> registerVehicle.execute(registerVehicleInput))
        .isInstanceOf(AlreadyExistsException.class)
        .hasMessage("Já existe um veículo com o chassi 9BWZZZ377LT004251");

    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void rejectsDuplicatePlate() {
    when(vehicleGateway.existsByPlate(new Plate("ABC1234"))).thenReturn(true);
    var registerVehicleInput = input(new BigDecimal("30000"), new BigDecimal("40000"));

    assertThatThrownBy(() -> registerVehicle.execute(registerVehicleInput))
        .isInstanceOf(AlreadyExistsException.class)
        .hasMessage("Já existe um veículo com a placa ABC1234");

    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void doesNotSaveVehicleWithZeroSalePrice() {
    var registerVehicleInput = input(new BigDecimal("30000"), BigDecimal.ZERO);

    assertThatThrownBy(() -> registerVehicle.execute(registerVehicleInput))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O valor deve ser maior que zero");

    verify(vehicleGateway, never()).save(any());
  }
}

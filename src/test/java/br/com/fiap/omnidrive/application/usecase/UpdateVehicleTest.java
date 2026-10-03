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
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link UpdateVehicle}.
 */
@ExtendWith(MockitoExtension.class)
class UpdateVehicleTest {

  private static final UUID VEHICLE_ID = UUID.randomUUID();

  private static final UUID SALE_ID = UUID.randomUUID();

  private static final long CURRENT_VERSION = 3;

  private static final UpdateVehicleInput INPUT = input(CURRENT_VERSION);

  @Mock
  private VehicleGateway vehicleGateway;

  private UpdateVehicle updateVehicle;

  private static Vehicle vehicle(
      VehicleStatus status,
      UUID currentSaleId
  ) {
    return Vehicle.restore(
        VEHICLE_ID,
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
        ),
        status,
        currentSaleId,
        CURRENT_VERSION
    );
  }

  private static UpdateVehicleInput input(Long version) {
    return input(
        "BRA2E19",
        version
    );
  }

  private static UpdateVehicleInput input(
      String plate,
      Long version
  ) {
    return new UpdateVehicleInput(
        plate,
        46000,
        "Volkswagen",
        "Gol",
        2020,
        "Branco",
        new BigDecimal("25000"),
        new BigDecimal("32000"),
        VehicleOrigin.CONSIGNED,
        version
    );
  }

  @BeforeEach
  void setUp() {
    updateVehicle = new UpdateVehicle(vehicleGateway);
  }

  @Test
  void updatesVehicleAndSavesIt() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.AVAILABLE, null)));
    when(vehicleGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var vehicle = updateVehicle.execute(VEHICLE_ID, INPUT);

    assertThat(vehicle.getId()).isEqualTo(VEHICLE_ID);
    assertThat(vehicle.getPlate()).isEqualTo(new Plate("BRA2E19"));
    assertThat(vehicle.getMileage()).isEqualTo(46000);
    assertThat(vehicle.getBrand()).isEqualTo("Volkswagen");
    assertThat(vehicle.getModel()).isEqualTo("Gol");
    assertThat(vehicle.getYear()).isEqualTo(2020);
    assertThat(vehicle.getColor()).isEqualTo("Branco");
    assertThat(vehicle.getPurchasePrice()).isEqualTo(Money.of("25000"));
    assertThat(vehicle.getSalePrice()).isEqualTo(Money.of("32000"));
    assertThat(vehicle.getOrigin()).isEqualTo(VehicleOrigin.CONSIGNED);
    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    verify(vehicleGateway).save(vehicle);
  }

  @Test
  void rejectsPlateOfAnotherVehicle() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.AVAILABLE, null)));
    when(vehicleGateway.existsByPlate(new Plate("BRA2E19"))).thenReturn(true);

    assertThatThrownBy(() -> updateVehicle.execute(VEHICLE_ID, INPUT))
        .isInstanceOf(AlreadyExistsException.class)
        .hasMessage("Já existe um veículo com a placa BRA2E19");
    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void keepingPlateDoesNotCheckDuplicates() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.AVAILABLE, null)));
    when(vehicleGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var samePlateInput = input("abc1d23", CURRENT_VERSION);

    updateVehicle.execute(VEHICLE_ID, samePlateInput);

    verify(vehicleGateway, never()).existsByPlate(any());
  }

  @Test
  void failsWhenVehicleDoesNotExist() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> updateVehicle.execute(VEHICLE_ID, INPUT))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(VEHICLE_ID.toString());
    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void doesNotSaveWhenStatusDoesNotAllowUpdate() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.RESERVED, SALE_ID)));

    assertThatThrownBy(() -> updateVehicle.execute(VEHICLE_ID, INPUT))
        .isInstanceOf(BusinessRuleException.class);
    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void rejectsUpdateBasedOnOutdatedVersion() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.AVAILABLE, null)));

    var outdatedInput = input(CURRENT_VERSION - 1);

    assertThatThrownBy(() -> updateVehicle.execute(VEHICLE_ID, outdatedInput))
        .isInstanceOf(ConcurrentUpdateException.class)
        .hasMessageContaining(VEHICLE_ID.toString());
    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void requiresVersion() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.AVAILABLE, null)));

    var inputWithoutVersion = input(null);

    assertThatThrownBy(() -> updateVehicle.execute(VEHICLE_ID, inputWithoutVersion))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("A versão do veículo é obrigatória");
    verify(vehicleGateway, never()).save(any());
  }
}

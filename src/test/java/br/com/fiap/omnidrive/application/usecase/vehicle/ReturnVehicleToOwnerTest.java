package br.com.fiap.omnidrive.application.usecase.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link ReturnVehicleToOwner}.
 */
@ExtendWith(MockitoExtension.class)
class ReturnVehicleToOwnerTest {

  private static final UUID VEHICLE_ID = UUID.randomUUID();

  @Mock
  private VehicleGateway vehicleGateway;

  private ReturnVehicleToOwner returnVehicleToOwner;

  private static Vehicle vehicle(
      VehicleOrigin origin,
      VehicleStatus status
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
            origin
        ),
        status,
        null,
        1
    );
  }

  @BeforeEach
  void setUp() {
    returnVehicleToOwner = new ReturnVehicleToOwner(vehicleGateway);
  }

  @ParameterizedTest
  @EnumSource(names = {"RECEIVED", "IN_PREPARATION", "AVAILABLE"})
  void returnsConsignedVehicleToOwnerAndSavesIt(VehicleStatus previous) {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleOrigin.CONSIGNED, previous)));
    when(vehicleGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var vehicle = returnVehicleToOwner.execute(VEHICLE_ID);

    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RETURNED_TO_OWNER);
    assertThat(vehicle.getStatusChanges())
        .singleElement()
        .satisfies(history -> {
          assertThat(history.previousStatus()).isEqualTo(previous);
          assertThat(history.newStatus()).isEqualTo(VehicleStatus.RETURNED_TO_OWNER);
        });
    verify(vehicleGateway).save(vehicle);
  }

  @Test
  void failsWhenVehicleDoesNotExist() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> returnVehicleToOwner.execute(VEHICLE_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(VEHICLE_ID.toString());
    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void doesNotSaveWhenStatusDoesNotAllowTransition() {
    when(vehicleGateway.findById(VEHICLE_ID))
        .thenReturn(Optional.of(vehicle(VehicleOrigin.CONSIGNED, VehicleStatus.RETURNED_TO_OWNER)));

    assertThatThrownBy(() -> returnVehicleToOwner.execute(VEHICLE_ID))
        .isInstanceOf(InvalidStatusTransitionException.class);
    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void doesNotSaveWhenVehicleIsOwned() {
    when(vehicleGateway.findById(VEHICLE_ID))
        .thenReturn(Optional.of(vehicle(VehicleOrigin.OWNED, VehicleStatus.AVAILABLE)));

    assertThatThrownBy(() -> returnVehicleToOwner.execute(VEHICLE_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("Só um veículo consignado pode ser devolvido ao dono");
    verify(vehicleGateway, never()).save(any());
  }
}

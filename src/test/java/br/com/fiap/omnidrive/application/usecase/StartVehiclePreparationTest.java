package br.com.fiap.omnidrive.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link StartVehiclePreparation}.
 */
@ExtendWith(MockitoExtension.class)
class StartVehiclePreparationTest {

  private static final UUID VEHICLE_ID = UUID.randomUUID();

  @Mock
  private VehicleGateway vehicleGateway;

  private StartVehiclePreparation startVehiclePreparation;

  private static Vehicle vehicle(VehicleStatus status) {
    return Vehicle.restore(
        VEHICLE_ID,
        "Fiat",
        "Uno",
        2020,
        "Prata",
        Money.of("30000"),
        Money.of("40000"),
        VehicleOrigin.OWNED,
        status,
        1
    );
  }

  @BeforeEach
  void setUp() {
    startVehiclePreparation = new StartVehiclePreparation(vehicleGateway);
  }

  @Test
  void movesReceivedVehicleToPreparationAndSavesIt() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.RECEIVED)));
    when(vehicleGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var vehicle = startVehiclePreparation.execute(VEHICLE_ID);

    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IN_PREPARATION);
    assertThat(vehicle.getStatusChanges())
        .singleElement()
        .satisfies(history -> {
          assertThat(history.previousStatus()).isEqualTo(VehicleStatus.RECEIVED);
          assertThat(history.newStatus()).isEqualTo(VehicleStatus.IN_PREPARATION);
        });
    verify(vehicleGateway).save(vehicle);
  }

  @Test
  void failsWhenVehicleDoesNotExist() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> startVehiclePreparation.execute(VEHICLE_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(VEHICLE_ID.toString());
    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void doesNotSaveWhenStatusDoesNotAllowTransition() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.AVAILABLE)));

    assertThatThrownBy(() -> startVehiclePreparation.execute(VEHICLE_ID))
        .isInstanceOf(InvalidStatusTransitionException.class);
    verify(vehicleGateway, never()).save(any());
  }
}

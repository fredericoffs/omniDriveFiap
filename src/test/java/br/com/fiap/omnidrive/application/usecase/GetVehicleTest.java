package br.com.fiap.omnidrive.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link GetVehicle}.
 */
@ExtendWith(MockitoExtension.class)
class GetVehicleTest {

  private static final UUID VEHICLE_ID = UUID.randomUUID();

  @Mock
  private VehicleGateway vehicleGateway;

  private GetVehicle getVehicle;

  @BeforeEach
  void setUp() {
    getVehicle = new GetVehicle(vehicleGateway);
  }

  @Test
  void returnsVehicleFoundByGateway() {
    var vehicle = Vehicle.restore(
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
        VehicleStatus.AVAILABLE,
        null,
        2
    );
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

    assertThat(getVehicle.execute(VEHICLE_ID)).isSameAs(vehicle);
  }

  @Test
  void failsWhenVehicleDoesNotExist() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getVehicle.execute(VEHICLE_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(VEHICLE_ID.toString());
  }
}

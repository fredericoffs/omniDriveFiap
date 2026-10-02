package br.com.fiap.omnidrive.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
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

  private static final UpdateVehicleInput INPUT = new UpdateVehicleInput(
      "Volkswagen",
      "Gol",
      2019,
      "Branco",
      new BigDecimal("25000"),
      new BigDecimal("32000"),
      VehicleOrigin.CONSIGNED
  );

  @Mock
  private VehicleGateway vehicleGateway;

  private UpdateVehicle updateVehicle;

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
        status
    );
  }

  @BeforeEach
  void setUp() {
    updateVehicle = new UpdateVehicle(vehicleGateway);
  }

  @Test
  void updatesVehicleAndSavesIt() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.AVAILABLE)));

    var vehicle = updateVehicle.execute(VEHICLE_ID, INPUT);

    assertThat(vehicle.getId()).isEqualTo(VEHICLE_ID);
    assertThat(vehicle.getBrand()).isEqualTo("Volkswagen");
    assertThat(vehicle.getModel()).isEqualTo("Gol");
    assertThat(vehicle.getYear()).isEqualTo(2019);
    assertThat(vehicle.getColor()).isEqualTo("Branco");
    assertThat(vehicle.getPurchasePrice()).isEqualTo(Money.of("25000"));
    assertThat(vehicle.getSalePrice()).isEqualTo(Money.of("32000"));
    assertThat(vehicle.getOrigin()).isEqualTo(VehicleOrigin.CONSIGNED);
    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    verify(vehicleGateway).save(vehicle);
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
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.RESERVED)));

    assertThatThrownBy(() -> updateVehicle.execute(VEHICLE_ID, INPUT))
        .isInstanceOf(BusinessRuleException.class);
    verify(vehicleGateway, never()).save(any());
  }
}

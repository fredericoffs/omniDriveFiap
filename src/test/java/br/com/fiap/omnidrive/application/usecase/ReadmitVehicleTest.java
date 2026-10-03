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
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
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
 * Testes do caso de uso {@link ReadmitVehicle}.
 */
@ExtendWith(MockitoExtension.class)
class ReadmitVehicleTest {

  private static final UUID VEHICLE_ID = UUID.randomUUID();

  private static final UUID SALE_ID = UUID.randomUUID();

  private static final ReadmitVehicleInput INPUT = new ReadmitVehicleInput(new BigDecimal("35000"));

  @Mock
  private VehicleGateway vehicleGateway;

  private ReadmitVehicle readmitVehicle;

  private static Vehicle vehicle(
      VehicleStatus status,
      UUID currentSaleId
  ) {
    return Vehicle.restore(
        VEHICLE_ID,
        new VehicleDetails(
            "Fiat",
            "Uno",
            2020,
            "Prata",
            Money.of("30000"),
            Money.of("40000"),
            VehicleOrigin.OWNED
        ),
        status,
        currentSaleId,
        4
    );
  }

  @BeforeEach
  void setUp() {
    readmitVehicle = new ReadmitVehicle(vehicleGateway);
  }

  @Test
  void readmitsSoldVehicleWithNewPurchasePriceAndSavesIt() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.SOLD, SALE_ID)));
    when(vehicleGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var vehicle = readmitVehicle.execute(VEHICLE_ID, INPUT);

    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RECEIVED);
    assertThat(vehicle.getPurchasePrice()).isEqualTo(Money.of("35000"));
    assertThat(vehicle.getSalePrice()).isEqualTo(Money.of("40000"));
    assertThat(vehicle.getStatusChanges())
        .singleElement()
        .satisfies(history -> {
          assertThat(history.previousStatus()).isEqualTo(VehicleStatus.SOLD);
          assertThat(history.newStatus()).isEqualTo(VehicleStatus.RECEIVED);
          assertThat(history.reason()).contains("30000.00");
        });
    verify(vehicleGateway).save(vehicle);
  }

  @Test
  void failsWhenVehicleDoesNotExist() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> readmitVehicle.execute(VEHICLE_ID, INPUT))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(VEHICLE_ID.toString());
    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void doesNotSaveWhenVehicleIsNotSold() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.AVAILABLE, null)));

    assertThatThrownBy(() -> readmitVehicle.execute(VEHICLE_ID, INPUT))
        .isInstanceOf(InvalidStatusTransitionException.class);
    verify(vehicleGateway, never()).save(any());
  }

  @Test
  void requiresNewPurchasePrice() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.SOLD, SALE_ID)));
    var inputWithoutPrice = new ReadmitVehicleInput(null);

    assertThatThrownBy(() -> readmitVehicle.execute(VEHICLE_ID, inputWithoutPrice))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O valor de entrada é obrigatório");
    verify(vehicleGateway, never()).save(any());
  }
}

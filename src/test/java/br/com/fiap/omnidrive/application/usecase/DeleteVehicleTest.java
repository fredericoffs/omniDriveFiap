package br.com.fiap.omnidrive.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
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
 * Testes do caso de uso {@link DeleteVehicle}.
 */
@ExtendWith(MockitoExtension.class)
class DeleteVehicleTest {

  private static final UUID VEHICLE_ID = UUID.randomUUID();

  @Mock
  private VehicleGateway vehicleGateway;

  @Mock
  private SaleGateway saleGateway;

  private DeleteVehicle deleteVehicle;

  private static Vehicle vehicle(VehicleStatus status) {
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
        status.isLinkedToSale() ? UUID.randomUUID() : null,
        1
    );
  }

  @BeforeEach
  void setUp() {
    deleteVehicle = new DeleteVehicle(vehicleGateway, saleGateway);
  }

  @Test
  void deletesReceivedVehicleWithoutSales() {
    var vehicle = vehicle(VehicleStatus.RECEIVED);
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
    when(saleGateway.existsByVehicleId(VEHICLE_ID)).thenReturn(false);

    deleteVehicle.execute(VEHICLE_ID);

    verify(vehicleGateway).delete(vehicle);
  }

  @Test
  void failsWhenVehicleDoesNotExist() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> deleteVehicle.execute(VEHICLE_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(VEHICLE_ID.toString());
    verify(vehicleGateway, never()).delete(any());
    verifyNoInteractions(saleGateway);
  }

  @Test
  void doesNotDeleteReceivedVehicleThatHadSales() {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.RECEIVED)));
    when(saleGateway.existsByVehicleId(VEHICLE_ID)).thenReturn(true);

    assertThatThrownBy(() -> deleteVehicle.execute(VEHICLE_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O veículo já teve venda e não pode ser excluído");
    verify(vehicleGateway, never()).delete(any());
  }

  @ParameterizedTest
  @EnumSource(names = "RECEIVED", mode = EnumSource.Mode.EXCLUDE)
  void doesNotDeleteVehicleOutsideReceived(VehicleStatus status) {
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(status)));

    assertThatThrownBy(() -> deleteVehicle.execute(VEHICLE_ID))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining(status.name());
    verify(vehicleGateway, never()).delete(any());
  }
}

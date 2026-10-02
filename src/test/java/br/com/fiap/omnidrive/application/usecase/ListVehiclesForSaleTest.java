package br.com.fiap.omnidrive.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link ListVehiclesForSale}.
 */
@ExtendWith(MockitoExtension.class)
class ListVehiclesForSaleTest {

  @Mock
  private VehicleGateway vehicleGateway;

  private ListVehiclesForSale listVehiclesForSale;

  private static Vehicle available(String salePrice) {
    return Vehicle.restore(
        UUID.randomUUID(),
        "Fiat",
        "Uno",
        2020,
        "Prata",
        Money.of("30000"),
        Money.of(salePrice),
        VehicleOrigin.OWNED,
        VehicleStatus.AVAILABLE,
        0
    );
  }

  @BeforeEach
  void setUp() {
    listVehiclesForSale = new ListVehiclesForSale(vehicleGateway);
  }

  @Test
  void listsOnlyAvailableVehiclesInGatewayOrder() {
    var vehicles = List.of(available("35000"), available("40000"));
    when(vehicleGateway.findByStatusOrderBySalePrice(VehicleStatus.AVAILABLE)).thenReturn(vehicles);

    assertThat(listVehiclesForSale.execute()).containsExactlyElementsOf(vehicles);
  }

  @Test
  void returnsEmptyListWhenNoVehicleIsAvailable() {
    when(vehicleGateway.findByStatusOrderBySalePrice(VehicleStatus.AVAILABLE)).thenReturn(List.of());

    assertThat(listVehiclesForSale.execute()).isEmpty();
  }
}

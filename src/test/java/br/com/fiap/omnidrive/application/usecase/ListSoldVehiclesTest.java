package br.com.fiap.omnidrive.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.sale.Sale;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link ListSoldVehicles}.
 */
@ExtendWith(MockitoExtension.class)
class ListSoldVehiclesTest {

  @Mock
  private VehicleGateway vehicleGateway;

  @Mock
  private SaleGateway saleGateway;

  private ListSoldVehicles listSoldVehicles;

  private static Vehicle sold(
      String plate,
      UUID saleId
  ) {
    return Vehicle.restore(
        UUID.randomUUID(),
        new Chassis("9BWZZZ377LT00" + plate.substring(3)),
        new VehicleDetails(
            "Fiat",
            "Uno",
            2020,
            "Prata",
            new Plate(plate),
            45000,
            Money.of("30000"),
            Money.of("40000"),
            VehicleOrigin.OWNED
        ),
        VehicleStatus.SOLD,
        saleId,
        5
    );
  }

  private static Sale sale(
      UUID id,
      Vehicle vehicle,
      String amount
  ) {
    return Sale.restore(
        id,
        vehicle.getId(),
        new Cpf("52998224725"),
        LocalDate.of(2026, 10, 1),
        Money.of(amount)
    );
  }

  @BeforeEach
  void setUp() {
    listSoldVehicles = new ListSoldVehicles(vehicleGateway, saleGateway);
  }

  @Test
  void pairsEachVehicleWithItsCurrentSaleKeepingGatewayOrder() {
    var cheapSaleId = UUID.randomUUID();
    var expensiveSaleId = UUID.randomUUID();
    var cheap = sold("ABC1001", cheapSaleId);
    var expensive = sold("ABC1002", expensiveSaleId);
    var cheapSale = sale(cheapSaleId, cheap, "20000");
    var expensiveSale = sale(expensiveSaleId, expensive, "50000");
    when(vehicleGateway.findSoldOrderBySaleAmount()).thenReturn(List.of(cheap, expensive));
    when(saleGateway.findAllById(List.of(cheapSaleId, expensiveSaleId)))
        .thenReturn(List.of(expensiveSale, cheapSale));

    var soldVehicles = listSoldVehicles.execute();

    assertThat(soldVehicles).containsExactly(
        new SoldVehicle(cheap, cheapSale),
        new SoldVehicle(expensive, expensiveSale)
    );
  }

  @Test
  void returnsEmptyListWhenNoVehicleIsSold() {
    when(vehicleGateway.findSoldOrderBySaleAmount()).thenReturn(List.of());
    when(saleGateway.findAllById(List.of())).thenReturn(List.of());

    assertThat(listSoldVehicles.execute()).isEmpty();
  }
}

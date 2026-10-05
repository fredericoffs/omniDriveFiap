package br.com.fiap.omnidrive.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.pagination.PageQuery;
import br.com.fiap.omnidrive.application.pagination.PageResult;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
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
        new Chassis("9BWZZZ377LT004251"),
        new VehicleDetails(
            "Fiat",
            "Uno",
            2020,
            "Prata",
            new Plate("ABC1D23"),
            45000,
            Money.of("30000"),
            Money.of(salePrice),
            VehicleOrigin.OWNED
        ),
        VehicleStatus.AVAILABLE,
        null,
        0
    );
  }

  @BeforeEach
  void setUp() {
    listVehiclesForSale = new ListVehiclesForSale(vehicleGateway);
  }

  @Test
  void returnsRequestedPageOfAvailableVehiclesFromGateway() {
    var pageQuery = new PageQuery(1, 2);
    var page = new PageResult<>(List.of(available("35000"), available("40000")), 1, 2, 5);
    when(vehicleGateway.findByStatusOrderBySalePrice(VehicleStatus.AVAILABLE, pageQuery)).thenReturn(page);

    assertThat(listVehiclesForSale.execute(pageQuery)).isEqualTo(page);
  }

  @Test
  void returnsEmptyPageWhenNoVehicleIsAvailable() {
    var pageQuery = PageQuery.first();
    var page = new PageResult<Vehicle>(List.of(), 0, 20, 0);
    when(vehicleGateway.findByStatusOrderBySalePrice(VehicleStatus.AVAILABLE, pageQuery)).thenReturn(page);

    assertThat(listVehiclesForSale.execute(pageQuery))
        .returns(List.of(), PageResult::content)
        .returns(0L, PageResult::totalElements);
  }
}

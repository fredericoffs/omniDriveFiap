package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.usecase.ListVehiclesForSale;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Testes da camada web das listagens de veículos.
 */
@WebMvcTest(VehicleListingController.class)
class VehicleListingControllerTest {

  private static final UUID VEHICLE_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private ListVehiclesForSale listVehiclesForSale;

  private static Vehicle vehicle(
      String salePrice,
      VehicleStatus status,
      long version
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
            Money.of(salePrice),
            VehicleOrigin.OWNED
        ),
        status,
        null,
        version
    );
  }

  @Test
  void listsVehiclesForSaleKeepingUseCaseOrder() {
    when(listVehiclesForSale.execute()).thenReturn(List.of(
        vehicle("20000", VehicleStatus.AVAILABLE, 1),
        vehicle("35000", VehicleStatus.AVAILABLE, 1)
    ));

    var response = mvc.get().uri("/vehicles/for-sale").exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$[*].salePrice")
        .asArray()
        .containsExactly(20000.00, 35000.00);
  }
}

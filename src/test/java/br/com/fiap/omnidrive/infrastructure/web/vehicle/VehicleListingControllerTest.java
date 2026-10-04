package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.usecase.ListSoldVehicles;
import br.com.fiap.omnidrive.application.usecase.ListVehiclesForSale;
import br.com.fiap.omnidrive.application.usecase.SoldVehicle;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Testes da camada web das listagens de veículos: contrato JSON e ordem definida pelos casos de uso.
 */
@WebMvcTest(VehicleListingController.class)
class VehicleListingControllerTest {

  private static final UUID VEHICLE_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private ListVehiclesForSale listVehiclesForSale;

  @MockitoBean
  private ListSoldVehicles listSoldVehicles;

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

  private static SoldVehicle soldVehicle(
      UUID saleId,
      String amount
  ) {
    var vehicle = Vehicle.restore(
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
            Money.of(amount),
            VehicleOrigin.OWNED
        ),
        VehicleStatus.SOLD,
        saleId,
        5
    );
    var sale = Sale.restore(
        saleId,
        vehicle.getId(),
        new Cpf("52998224725"),
        LocalDate.of(2026, 10, 1),
        Money.of(amount)
    );
    return new SoldVehicle(vehicle, sale);
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

  @Test
  void listsSoldVehiclesWithCurrentSaleKeepingUseCaseOrder() {
    var saleId = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    when(listSoldVehicles.execute()).thenReturn(List.of(
        soldVehicle(saleId, "20000"),
        soldVehicle(UUID.randomUUID(), "35000")
    ));

    var response = mvc.get().uri("/vehicles/sold").exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            [
              {
                "vehicle": {
                  "brand": "Fiat",
                  "salePrice": 20000.00,
                  "status": "SOLD"
                },
                "sale": {
                  "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
                  "saleDate": "2026-10-01",
                  "amount": 20000.00,
                  "buyerCpf": "52998224725"
                }
              },
              {
                "sale": {
                  "amount": 35000.00
                }
              }
            ]
            """);
    assertThat(response)
        .bodyJson()
        .extractingPath("$[*].sale.amount")
        .asArray()
        .containsExactly(20000.00, 35000.00);
  }
}

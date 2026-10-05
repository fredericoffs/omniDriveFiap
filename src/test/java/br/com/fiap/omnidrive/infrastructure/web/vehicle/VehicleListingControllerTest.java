package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.pagination.PageQuery;
import br.com.fiap.omnidrive.application.pagination.PageResult;
import br.com.fiap.omnidrive.application.usecase.ListSoldVehicles;
import br.com.fiap.omnidrive.application.usecase.SoldVehicle;
import br.com.fiap.omnidrive.application.usecase.vehicle.ListVehiclesForSale;
import br.com.fiap.omnidrive.application.usecase.vehicle.SearchVehicles;
import br.com.fiap.omnidrive.application.usecase.vehicle.SearchVehiclesInput;
import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.sale.Sale;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Testes da camada web das listagens de veículos: repasse dos filtros da busca, contrato JSON da página, ordem
 * definida pelos casos de uso e validação dos parâmetros.
 */
@WebMvcTest(VehicleListingController.class)
class VehicleListingControllerTest {

  private static final UUID VEHICLE_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private SearchVehicles searchVehicles;

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

  private static <T> PageResult<T> firstPage(List<T> content) {
    return new PageResult<>(content, 0, PageQuery.DEFAULT_SIZE, content.size());
  }

  @Test
  void searchPassesFiltersAndRequestedPage() {
    var input = new SearchVehiclesInput(
        "abc-1234",
        "9BWZZZ377LT004251",
        Set.of(VehicleStatus.RECEIVED, VehicleStatus.SOLD),
        "hon",
        "civ"
    );
    when(searchVehicles.execute(input, new PageQuery(1, 5))).thenReturn(new PageResult<>(
        List.of(vehicle("40000", VehicleStatus.RECEIVED, 0)),
        1,
        5,
        6
    ));

    var response = mvc.get()
        .uri("/vehicles?plate=abc-1234&chassis=9BWZZZ377LT004251&status=RECEIVED&status=SOLD&brand=hon&model=civ"
            + "&page=1&size=5")
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "content": [
                {
                  "id": "0f8fad5b-d9cb-469f-a165-70867728950e",
                  "status": "RECEIVED"
                }
              ],
              "page": 1,
              "size": 5,
              "totalElements": 6,
              "totalPages": 2
            }
            """);
  }

  @Test
  void searchWithoutFiltersUsesFirstPage() {
    var input = new SearchVehiclesInput(null, null, null, null, null);
    when(searchVehicles.execute(input, PageQuery.first())).thenReturn(firstPage(List.of()));

    var response = mvc.get().uri("/vehicles").exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.totalElements")
        .isEqualTo(0);
  }

  @Test
  void searchRejectsInvalidPlateWith422() {
    when(searchVehicles.execute(any(), any()))
        .thenThrow(new BusinessRuleException("A placa deve seguir o padrão antigo (ABC-1234) ou o Mercosul (ABC1D23)"));

    var response = mvc.get().uri("/vehicles?plate=AB-12").exchange();

    assertThat(response)
        .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("A placa deve seguir o padrão antigo (ABC-1234) ou o Mercosul (ABC1D23)");
  }

  @Test
  void listsVehiclesForSaleKeepingUseCaseOrder() {
    when(listVehiclesForSale.execute(PageQuery.first())).thenReturn(firstPage(List.of(
        vehicle("20000", VehicleStatus.AVAILABLE, 1),
        vehicle("35000", VehicleStatus.AVAILABLE, 1)
    )));

    var response = mvc.get().uri("/vehicles/for-sale").exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.content[*].salePrice")
        .asArray()
        .containsExactly(20000.00, 35000.00);
  }

  @Test
  void passesRequestedPageAndReturnsPaginationData() {
    var pageQuery = new PageQuery(2, 5);
    when(listVehiclesForSale.execute(pageQuery)).thenReturn(new PageResult<>(
        List.of(vehicle("20000", VehicleStatus.AVAILABLE, 1)),
        2,
        5,
        11
    ));

    var response = mvc.get().uri("/vehicles/for-sale?page=2&size=5").exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "content": [
                {
                  "salePrice": 20000.00
                }
              ],
              "page": 2,
              "size": 5,
              "totalElements": 11,
              "totalPages": 3
            }
            """);
  }

  @Test
  void returnsEmptyContentWhenNoVehicleIsForSale() {
    when(listVehiclesForSale.execute(PageQuery.first())).thenReturn(firstPage(List.of()));

    var response = mvc.get().uri("/vehicles/for-sale").exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "content": [],
              "page": 0,
              "size": 20,
              "totalElements": 0,
              "totalPages": 0
            }
            """);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', value = {
      "/vehicles/for-sale?page=-1 | page | A página deve ser maior ou igual a zero",
      "/vehicles/for-sale?size=0  | size | O tamanho da página deve estar entre 1 e 100",
      "/vehicles/for-sale?size=101 | size | O tamanho da página deve estar entre 1 e 100",
      "/vehicles/for-sale?page=abc | page | Valor inválido",
      "/vehicles/sold?page=-1 | page | A página deve ser maior ou igual a zero",
      "/vehicles/sold?size=101 | size | O tamanho da página deve estar entre 1 e 100",
      "/vehicles?status=XYZ | status | Valor inválido",
      "/vehicles?status=AVAILABLE&size=0 | size | O tamanho da página deve estar entre 1 e 100"
  })
  void rejectsInvalidPaginationParameters(
      String uri,
      String parameter,
      String message
  ) {
    var response = mvc.get().uri(uri).exchange();

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.errors." + parameter)
        .isEqualTo(message);
    verifyNoInteractions(searchVehicles, listVehiclesForSale, listSoldVehicles);
  }

  @Test
  void passesRequestedPageToSoldVehicles() {
    var pageQuery = new PageQuery(1, 2);
    when(listSoldVehicles.execute(pageQuery)).thenReturn(new PageResult<>(List.of(), 1, 2, 2));

    var response = mvc.get().uri("/vehicles/sold?page=1&size=2").exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "content": [],
              "page": 1,
              "size": 2,
              "totalElements": 2,
              "totalPages": 1
            }
            """);
  }

  @Test
  void listsSoldVehiclesWithCurrentSaleKeepingUseCaseOrder() {
    var saleId = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");
    when(listSoldVehicles.execute(PageQuery.first())).thenReturn(firstPage(List.of(
        soldVehicle(saleId, "20000"),
        soldVehicle(UUID.randomUUID(), "35000")
    )));

    var response = mvc.get().uri("/vehicles/sold").exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "content": [
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
              ],
              "totalElements": 2
            }
            """);
    assertThat(response)
        .bodyJson()
        .extractingPath("$.content[*].sale.amount")
        .asArray()
        .containsExactly(20000.00, 35000.00);
  }
}

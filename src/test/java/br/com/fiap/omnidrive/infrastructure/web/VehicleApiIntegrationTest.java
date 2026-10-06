package br.com.fiap.omnidrive.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.omnidrive.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Testes de ponta a ponta da API de veículos, com a aplicação completa e o Postgres do Testcontainers. Também
 * verifica que a documentação OpenAPI é publicada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VehicleApiIntegrationTest {

  @Autowired
  private MockMvcTester mvc;

  private static String body(
      String plate,
      String brand,
      Long version
  ) {
    return """
        {
          "chassis": "9BWZZZ377LT00%s",
          "plate": "%s",
          "mileage": 45000,
          "brand": "%s",
          "model": "Uno",
          "year": 2020,
          "color": "Prata",
          "purchasePrice": 30000,
          "salePrice": 40000,
          "origin": "OWNED"%s
        }
        """.formatted(plate.substring(3), plate, brand, version == null ? "" : ", \"version\": " + version);
  }

  @Test
  void registersGetsAndUpdatesVehicleRejectingOutdatedVersion() {
    var created = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("FIA1001", "Fiat", null))
        .exchange();
    assertThat(created)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "status": "RECEIVED",
              "version": 0
            }
            """);
    var location = created.getResponse().getHeader("Location");

    assert location != null;
    var found = mvc.get().uri(location).exchange();
    assertThat(found)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "brand": "Fiat",
              "status": "RECEIVED",
              "version": 0
            }
            """);

    var updated = mvc.put().uri(location)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("FIA1001", "Volkswagen", 0L))
        .exchange();
    assertThat(updated)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "brand": "Volkswagen",
              "version": 1
            }
            """);

    var outdated = mvc.put().uri(location)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("FIA1001", "Chevrolet", 0L))
        .exchange();
    assertThat(outdated).hasStatus(HttpStatus.CONFLICT);

    var reread = mvc.get().uri(location).exchange();
    assertThat(reread)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.version")
        .isEqualTo(1);
  }

  @Test
  void preparesAndReleasesVehicleUntilItIsListedForSale() {
    var location = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("REN1002", "Renault", null))
        .exchange()
        .getResponse()
        .getHeader("Location");

    var releasedTooEarly = mvc.post().uri(location + "/release").exchange();
    assertThat(releasedTooEarly).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);

    var prepared = mvc.post().uri(location + "/preparation").exchange();
    assertThat(prepared)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("IN_PREPARATION");

    var released = mvc.post().uri(location + "/release").exchange();
    assertThat(released)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "status": "AVAILABLE",
              "version": 2
            }
            """);

    var forSale = mvc.get().uri("/vehicles/for-sale?size=100").exchange();
    assertThat(forSale)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.content[*].brand")
        .asArray()
        .contains("Renault");
  }

  @Test
  void withdrawsAvailableVehicleForRepairAndReleasesItAgain() {
    var location = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("REP1008", "Volvo", null))
        .exchange()
        .getResponse()
        .getHeader("Location");
    assert location != null;
    var vehicleId = location.substring(location.lastIndexOf('/') + 1);
    assertThat(mvc.post().uri(location + "/preparation").exchange()).hasStatus(HttpStatus.OK);
    assertThat(mvc.post().uri(location + "/release").exchange()).hasStatus(HttpStatus.OK);

    var withdrawn = mvc.post().uri(location + "/preparation").exchange();
    assertThat(withdrawn)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("IN_PREPARATION");
    assertThat(mvc.get().uri("/vehicles/for-sale?size=100").exchange())
        .bodyJson()
        .extractingPath("$.content[*].id")
        .asArray()
        .doesNotContain(vehicleId);

    assertThat(mvc.post().uri(location + "/release").exchange()).hasStatus(HttpStatus.OK);
    assertThat(mvc.get().uri("/vehicles/for-sale?size=100").exchange())
        .bodyJson()
        .extractingPath("$.content[*].id")
        .asArray()
        .contains(vehicleId);
  }

  @Test
  void returnsConsignedVehicleToOwnerAndReadmitsItWithSameMileage() {
    var location = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("DEV1009", "Honda", null).replace("\"OWNED\"", "\"CONSIGNED\""))
        .exchange()
        .getResponse()
        .getHeader("Location");
    assert location != null;
    var vehicleId = location.substring(location.lastIndexOf('/') + 1);
    assertThat(mvc.post().uri(location + "/preparation").exchange()).hasStatus(HttpStatus.OK);
    assertThat(mvc.post().uri(location + "/release").exchange()).hasStatus(HttpStatus.OK);

    var returned = mvc.post().uri(location + "/return-to-owner").exchange();
    assertThat(returned)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("RETURNED_TO_OWNER");
    assertThat(mvc.get().uri("/vehicles/for-sale?size=100").exchange())
        .bodyJson()
        .extractingPath("$.content[*].id")
        .asArray()
        .doesNotContain(vehicleId);

    var readmitted = mvc.post().uri(location + "/readmission")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "purchasePrice": 32000,
              "mileage": 45000
            }
            """)
        .exchange();
    assertThat(readmitted)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "status": "RECEIVED",
              "purchasePrice": 32000.00
            }
            """);
  }

  @Test
  void rejectsReturnToOwnerOfOwnedVehicle() {
    var location = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("DEV1010", "Honda", null))
        .exchange()
        .getResponse()
        .getHeader("Location");
    assert location != null;

    var rejected = mvc.post().uri(location + "/return-to-owner").exchange();

    assertThat(rejected)
        .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Só um veículo consignado pode ser devolvido ao dono");
    assertThat(mvc.get().uri(location).exchange())
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("RECEIVED");
  }

  @Test
  void rejectsReadmissionOfVehicleThatWasNotSold() {
    var location = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("PEU1003", "Peugeot", null))
        .exchange()
        .getResponse()
        .getHeader("Location");

    var readmitted = mvc.post().uri(location + "/readmission")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "purchasePrice": 35000,
              "mileage": 52000
            }
            """)
        .exchange();

    assertThat(readmitted)
        .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
        .bodyJson()
        .extractingPath("$.title")
        .isEqualTo("Regra de negócio violada");
  }

  @Test
  void rejectsDuplicateChassisAndPlate() {
    var first = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("DUP1004", "Fiat", null))
        .exchange();
    assertThat(first).hasStatus(HttpStatus.CREATED);

    var duplicate = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("DUP1004", "Fiat", null))
        .exchange();

    assertThat(duplicate)
        .hasStatus(HttpStatus.CONFLICT)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "title": "Registro duplicado",
              "detail": "Já existe um veículo com o chassi 9BWZZZ377LT001004"
            }
            """);
  }

  @Test
  void returnsNotFoundForUnknownVehicle() {
    var response = mvc.put().uri("/vehicles/{id}", "6b1f7a4e-3c1d-4a8e-9a4f-2f1d1c0b9e7a")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("UNK1005", "Fiat", 0L))
        .exchange();

    assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void deletesReceivedVehicleAndRejectsDeletionAfterPreparation() {
    var location = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("DEL1006", "Fiat", null))
        .exchange()
        .getResponse()
        .getHeader("Location");
    assert location != null;

    assertThat(mvc.delete().uri(location).exchange()).hasStatus(HttpStatus.NO_CONTENT);
    assertThat(mvc.get().uri(location).exchange()).hasStatus(HttpStatus.NOT_FOUND);
    assertThat(mvc.delete().uri(location).exchange()).hasStatus(HttpStatus.NOT_FOUND);

    var preparedLocation = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("DEL1007", "Fiat", null))
        .exchange()
        .getResponse()
        .getHeader("Location");
    assert preparedLocation != null;
    assertThat(mvc.post().uri(preparedLocation + "/preparation").exchange()).hasStatus(HttpStatus.OK);

    var rejected = mvc.delete().uri(preparedLocation).exchange();
    assertThat(rejected)
        .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("O veículo não pode ser excluído com status IN_PREPARATION");
    assertThat(mvc.get().uri(preparedLocation).exchange()).hasStatus(HttpStatus.OK);
  }

  @Test
  void findsRegisteredVehicleBySearch() {
    var created = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("BUS1005", "Chevrolet", null))
        .exchange();
    assertThat(created).hasStatus(HttpStatus.CREATED);

    var byPlate = mvc.get().uri("/vehicles?plate=bus-1005").exchange();
    assertThat(byPlate)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "content": [
                {
                  "plate": "BUS1005",
                  "brand": "Chevrolet",
                  "status": "RECEIVED"
                }
              ],
              "totalElements": 1
            }
            """);

    var byStatusAndBrand = mvc.get().uri("/vehicles?status=RECEIVED&brand=chevro&size=100").exchange();
    assertThat(byStatusAndBrand)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.content[*].plate")
        .asArray()
        .contains("BUS1005");

    var otherStatus = mvc.get().uri("/vehicles?plate=BUS1005&status=AVAILABLE").exchange();
    assertThat(otherStatus)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.totalElements")
        .isEqualTo(0);
  }

  @Test
  void publishesOpenApiDocumentation() {
    var docs = mvc.get().uri("/v3/api-docs").exchange();

    assertThat(docs)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "info": {
                "title": "OmniDrive API"
              },
              "paths": {
                "/vehicles": {
                  "post": {
                    "summary": "Cadastrar veículo"
                  },
                  "get": {
                    "summary": "Buscar veículos"
                  }
                },
                "/vehicles/{id}": {
                  "delete": {
                    "summary": "Excluir veículo"
                  }
                }
              }
            }
            """);
    assertThat(docs)
        .bodyJson()
        .extractingPath("$.paths['/vehicles/for-sale'].get.parameters[*].name")
        .asArray()
        .containsExactly("page", "size");
    assertThat(mvc.get().uri("/swagger-ui/index.html").exchange()).hasStatus(HttpStatus.OK);
  }
}

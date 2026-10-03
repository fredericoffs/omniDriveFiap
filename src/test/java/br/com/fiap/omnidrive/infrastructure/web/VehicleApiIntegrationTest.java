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

    var forSale = mvc.get().uri("/vehicles/for-sale").exchange();
    assertThat(forSale)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$[*].brand")
        .asArray()
        .contains("Renault");
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
                  }
                }
              }
            }
            """);
    assertThat(mvc.get().uri("/swagger-ui/index.html").exchange()).hasStatus(HttpStatus.OK);
  }
}

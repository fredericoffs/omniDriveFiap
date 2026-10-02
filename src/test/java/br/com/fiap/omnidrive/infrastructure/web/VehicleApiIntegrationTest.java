package br.com.fiap.omnidrive.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.omnidrive.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
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
      String brand,
      Long version
  ) {
    return """
        {
          "brand": "%s",
          "model": "Uno",
          "year": 2020,
          "color": "Prata",
          "purchasePrice": 30000,
          "salePrice": 40000,
          "origin": "OWNED"%s
        }
        """.formatted(brand, version == null ? "" : ", \"version\": " + version);
  }

  @Test
  void registersAndUpdatesVehicleRejectingOutdatedVersion() throws Exception {
    var created = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("Fiat", null))
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
    var id = JsonPath.read(created.getResponse().getContentAsString(), "$.id").toString();

    var updated = mvc.put().uri("/vehicles/{id}", id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("Volkswagen", 0L))
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

    var outdated = mvc.put().uri("/vehicles/{id}", id)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("Chevrolet", 0L))
        .exchange();
    assertThat(outdated).hasStatus(HttpStatus.CONFLICT);
  }

  @Test
  void returnsNotFoundForUnknownVehicle() {
    var response = mvc.put().uri("/vehicles/{id}", "6b1f7a4e-3c1d-4a8e-9a4f-2f1d1c0b9e7a")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body("Fiat", 0L))
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

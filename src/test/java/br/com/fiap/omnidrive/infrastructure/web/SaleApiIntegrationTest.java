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
 * Testes de ponta a ponta da API de vendas, com a aplicação completa e o Postgres do Testcontainers.
 *
 * <p>Pela API, um veículo só chega a Vendido com o webhook de pagamento; até lá, os testes conferem que um veículo
 * Reservado fica fora das listagens.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SaleApiIntegrationTest {

  @Autowired
  private MockMvcTester mvc;

  private static String saleBody(String vehicleId) {
    return """
        {
          "vehicleId": "%s",
          "buyerCpf": "529.982.247-25",
          "saleDate": "2026-10-01"
        }
        """.formatted(vehicleId);
  }

  @Test
  void sellsAvailableVehicleReservingItAndRemovingItFromListings() {
    var vehicleId = availableVehicle("SAL2001", "Citroen");

    var sold = mvc.post().uri("/sales")
        .contentType(MediaType.APPLICATION_JSON)
        .content(saleBody(vehicleId))
        .exchange();
    assertThat(sold)
        .hasStatus(HttpStatus.CREATED)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "vehicleId": "%s",
              "buyerCpf": "52998224725",
              "saleDate": "2026-10-01",
              "amount": 40000.00,
              "paymentStatus": "AWAITING_PAYMENT"
            }
            """.formatted(vehicleId));
    assertThat(sold.getResponse().getHeader("Location")).contains("/sales/");

    var vehicle = mvc.get().uri("/vehicles/{id}", vehicleId).exchange();
    assertThat(vehicle)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("RESERVED");

    var forSale = mvc.get().uri("/vehicles/for-sale").exchange();
    assertThat(forSale)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$[*].id")
        .asArray()
        .doesNotContain(vehicleId);

    var soldVehicles = mvc.get().uri("/vehicles/sold").exchange();
    assertThat(soldVehicles)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$[*].vehicle.id")
        .asArray()
        .doesNotContain(vehicleId);
  }

  @Test
  void rejectsSecondSaleOfReservedVehicle() {
    var vehicleId = availableVehicle("SAL2002", "Honda");
    var first = mvc.post().uri("/sales")
        .contentType(MediaType.APPLICATION_JSON)
        .content(saleBody(vehicleId))
        .exchange();
    assertThat(first).hasStatus(HttpStatus.CREATED);

    var second = mvc.post().uri("/sales")
        .contentType(MediaType.APPLICATION_JSON)
        .content(saleBody(vehicleId))
        .exchange();

    assertThat(second)
        .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
        .bodyJson()
        .extractingPath("$.title")
        .isEqualTo("Regra de negócio violada");
  }

  @Test
  void returnsNotFoundForUnknownVehicle() {
    var response = mvc.post().uri("/sales")
        .contentType(MediaType.APPLICATION_JSON)
        .content(saleBody("6b1f7a4e-3c1d-4a8e-9a4f-2f1d1c0b9e7a"))
        .exchange();

    assertThat(response).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void publishesSaleEndpointInOpenApiDocumentation() {
    var docs = mvc.get().uri("/v3/api-docs").exchange();

    assertThat(docs)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "paths": {
                "/sales": {
                  "post": {
                    "summary": "Efetuar venda"
                  }
                }
              }
            }
            """);
  }

  private String availableVehicle(
      String plate,
      String brand
  ) {
    var created = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "chassis": "9BWZZZ377LT00%s",
              "plate": "%s",
              "mileage": 45000,
              "brand": "%s",
              "model": "City",
              "year": 2020,
              "color": "Preto",
              "purchasePrice": 30000,
              "salePrice": 40000,
              "origin": "OWNED"
            }
            """.formatted(plate.substring(3), plate, brand))
        .exchange();
    assertThat(created).hasStatus(HttpStatus.CREATED);
    var location = created.getResponse().getHeader("Location");
    assertThat(mvc.post().uri(location + "/preparation").exchange()).hasStatus(HttpStatus.OK);
    assertThat(mvc.post().uri(location + "/release").exchange()).hasStatus(HttpStatus.OK);
    assert location != null;
    return location.substring(location.lastIndexOf('/') + 1);
  }
}

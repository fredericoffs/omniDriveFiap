package br.com.fiap.omnidrive.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.omnidrive.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Testes de ponta a ponta do webhook de pagamento, com a aplicação completa e o Postgres do Testcontainers: da venda
 * ao veículo vendido, à repetição, ao estorno e à recusa.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PaymentWebhookApiIntegrationTest {

  @Autowired
  private MockMvcTester mvc;

  private static String webhookBody(
      String paymentCode,
      String status
  ) {
    return """
        {
          "paymentCode": "%s",
          "status": "%s"
        }
        """.formatted(paymentCode, status);
  }

  @Test
  void paidSaleListsVehicleAsSoldAndRefundReturnsItToSale() throws UnsupportedEncodingException {
    var vehicleId = availableVehicle("WEB3001", "Toyota");
    var paymentCode = sell(vehicleId);

    var paid = sendWebhook(paymentCode, "PAID");
    assertThat(paid)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("PAID");
    assertThat(vehicleStatus(vehicleId)).isEqualTo("SOLD");
    assertThat(mvc.get().uri("/vehicles/sold?size=100").exchange())
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.content[*].vehicle.id")
        .asArray()
        .contains(vehicleId);

    var repeated = sendWebhook(paymentCode, "PAID");
    assertThat(repeated)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("PAID");

    var refunded = sendWebhook(paymentCode, "CANCELED");
    assertThat(refunded)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("REFUNDED");
    assertThat(vehicleStatus(vehicleId)).isEqualTo("AVAILABLE");
    assertThat(mvc.get().uri("/vehicles/sold?size=100").exchange())
        .bodyJson()
        .extractingPath("$.content[*].vehicle.id")
        .asArray()
        .doesNotContain(vehicleId);
    assertThat(mvc.get().uri("/vehicles/for-sale?size=100").exchange())
        .bodyJson()
        .extractingPath("$.content[*].id")
        .asArray()
        .contains(vehicleId);
  }

  @Test
  void declinedSaleReturnsVehicleToSaleAndRejectsLaterPayment() throws UnsupportedEncodingException {
    var vehicleId = availableVehicle("WEB3002", "Nissan");
    var paymentCode = sell(vehicleId);

    var declined = sendWebhook(paymentCode, "DECLINED");
    assertThat(declined)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("DECLINED");
    assertThat(vehicleStatus(vehicleId)).isEqualTo("AVAILABLE");

    var paidAfterDeclined = sendWebhook(paymentCode, "PAID");
    assertThat(paidAfterDeclined)
        .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
        .bodyJson()
        .extractingPath("$.title")
        .isEqualTo("Regra de negócio violada");
    assertThat(vehicleStatus(vehicleId)).isEqualTo("AVAILABLE");
  }

  @Test
  void returnsNotFoundForUnknownPaymentCode() {
    assertThat(sendWebhook("unknown-code", "PAID")).hasStatus(HttpStatus.NOT_FOUND);
  }

  @Test
  void publishesWebhookInOpenApiDocumentation() {
    assertThat(mvc.get().uri("/v3/api-docs").exchange())
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "paths": {
                "/webhooks/payments": {
                  "post": {
                    "summary": "Informar resultado do pagamento"
                  }
                }
              }
            }
            """);
  }

  private MvcTestResult sendWebhook(
      String paymentCode,
      String status
  ) {
    return mvc.post().uri("/webhooks/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .content(webhookBody(paymentCode, status))
        .exchange();
  }

  private String vehicleStatus(String vehicleId) throws UnsupportedEncodingException {
    var body = mvc.get().uri("/vehicles/{id}", vehicleId).exchange().getResponse().getContentAsString();
    return JsonPath.read(body, "$.status");
  }

  private String sell(String vehicleId) throws UnsupportedEncodingException {
    var sold = mvc.post().uri("/sales")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "vehicleId": "%s",
              "buyerCpf": "529.982.247-25",
              "saleDate": "2026-10-01"
            }
            """.formatted(vehicleId))
        .exchange();
    assertThat(sold).hasStatus(HttpStatus.CREATED);
    return JsonPath.read(sold.getResponse().getContentAsString(), "$.paymentCode");
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
              "model": "Corolla",
              "year": 2020,
              "color": "Branco",
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

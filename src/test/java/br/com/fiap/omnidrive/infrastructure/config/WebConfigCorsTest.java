package br.com.fiap.omnidrive.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.usecase.sale.ProcessPaymentResult;
import br.com.fiap.omnidrive.application.usecase.vehicle.DeleteVehicle;
import br.com.fiap.omnidrive.application.usecase.vehicle.GetVehicle;
import br.com.fiap.omnidrive.application.usecase.vehicle.RegisterVehicle;
import br.com.fiap.omnidrive.application.usecase.vehicle.UpdateVehicle;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import br.com.fiap.omnidrive.infrastructure.web.vehicle.VehicleController;
import br.com.fiap.omnidrive.infrastructure.web.webhook.PaymentWebhookController;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Testes do CORS configurado no {@link WebConfig}, com as origens padrão de desenvolvimento do
 * {@code application.properties}: preflight de origem permitida e não permitida, header {@code Location} exposto e
 * webhook sem CORS.
 */
@WebMvcTest({VehicleController.class, PaymentWebhookController.class})
class WebConfigCorsTest {

  private static final String ALLOWED_ORIGIN = "http://localhost:5173";

  private static final UUID VEHICLE_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private RegisterVehicle registerVehicle;

  @MockitoBean
  private GetVehicle getVehicle;

  @MockitoBean
  private UpdateVehicle updateVehicle;

  @MockitoBean
  private DeleteVehicle deleteVehicle;

  @MockitoBean
  private ProcessPaymentResult processPaymentResult;

  private static Vehicle vehicle() {
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
            Money.of("40000"),
            VehicleOrigin.OWNED
        ),
        VehicleStatus.RECEIVED,
        null,
        0
    );
  }

  @Test
  void answersPreflightFromAllowedOrigin() {
    var response = mvc.options().uri("/vehicles")
        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type")
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN)
        .hasHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600")
        .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
    assertThat(response.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
        .contains("GET", "POST", "PUT", "DELETE");
  }

  @Test
  void rejectsPreflightFromOriginNotAllowed() {
    var response = mvc.options().uri("/vehicles")
        .header(HttpHeaders.ORIGIN, "https://outro-site.example.com")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.FORBIDDEN)
        .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
  }

  @Test
  void exposesLocationHeaderToAllowedOrigin() {
    when(getVehicle.execute(VEHICLE_ID)).thenReturn(vehicle());

    var response = mvc.get().uri("/vehicles/{id}", VEHICLE_ID)
        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN)
        .hasHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.LOCATION);
  }

  @Test
  void doesNotEnableCorsOnWebhook() {
    var response = mvc.options().uri("/webhooks/payments")
        .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    verifyNoInteractions(processPaymentResult);
  }
}

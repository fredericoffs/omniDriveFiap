package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.usecase.vehicle.ReadmitVehicle;
import br.com.fiap.omnidrive.application.usecase.vehicle.ReadmitVehicleInput;
import br.com.fiap.omnidrive.application.usecase.vehicle.ReleaseVehicleForSale;
import br.com.fiap.omnidrive.application.usecase.vehicle.StartVehiclePreparation;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Testes da camada web do ciclo de status do veículo: preparação, liberação para venda e reentrada.
 */
@WebMvcTest(VehicleStatusController.class)
class VehicleStatusControllerTest {

  private static final UUID VEHICLE_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private StartVehiclePreparation startVehiclePreparation;

  @MockitoBean
  private ReleaseVehicleForSale releaseVehicleForSale;

  @MockitoBean
  private ReadmitVehicle readmitVehicle;

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
  void startsPreparation() {
    when(startVehiclePreparation.execute(VEHICLE_ID)).thenReturn(vehicle("40000", VehicleStatus.IN_PREPARATION, 1));

    var response = mvc.post().uri("/vehicles/{id}/preparation", VEHICLE_ID).exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "id": "0f8fad5b-d9cb-469f-a165-70867728950e",
              "status": "IN_PREPARATION",
              "version": 1
            }
            """);
  }

  @Test
  void releasesForSale() {
    when(releaseVehicleForSale.execute(VEHICLE_ID)).thenReturn(vehicle("40000", VehicleStatus.AVAILABLE, 2));

    var response = mvc.post().uri("/vehicles/{id}/release", VEHICLE_ID).exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "id": "0f8fad5b-d9cb-469f-a165-70867728950e",
              "status": "AVAILABLE",
              "version": 2
            }
            """);
  }

  @Test
  void returnsUnprocessableContentForInvalidStatusTransition() {
    when(releaseVehicleForSale.execute(VEHICLE_ID))
        .thenThrow(new InvalidStatusTransitionException("Transição inválida de RECEIVED para AVAILABLE"));

    var response = mvc.post().uri("/vehicles/{id}/release", VEHICLE_ID).exchange();

    assertThat(response)
        .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "status": 422,
              "title": "Regra de negócio violada",
              "detail": "Transição inválida de RECEIVED para AVAILABLE"
            }
            """);
  }

  @Test
  void readmitsVehicleSendingNewPurchasePriceToUseCase() {
    when(readmitVehicle.execute(eq(VEHICLE_ID), any())).thenReturn(vehicle("40000", VehicleStatus.RECEIVED, 5));

    var response = mvc.post().uri("/vehicles/{id}/readmission", VEHICLE_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "purchasePrice": 35000.00,
              "mileage": 52000
            }
            """)
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "status": "RECEIVED",
              "version": 5
            }
            """);
    verify(readmitVehicle).execute(VEHICLE_ID, new ReadmitVehicleInput(
        new BigDecimal("35000.00"),
        52000
    ));
  }

  @Test
  void requiresPurchasePriceOnReadmission() {
    var response = mvc.post().uri("/vehicles/{id}/readmission", VEHICLE_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}")
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.errors.purchasePrice")
        .isEqualTo("O valor de entrada é obrigatório");
    verifyNoInteractions(readmitVehicle);
  }
}

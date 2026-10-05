package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.usecase.vehicle.DeleteVehicle;
import br.com.fiap.omnidrive.application.usecase.vehicle.GetVehicle;
import br.com.fiap.omnidrive.application.usecase.vehicle.RegisterVehicle;
import br.com.fiap.omnidrive.application.usecase.vehicle.UpdateVehicle;
import br.com.fiap.omnidrive.application.usecase.vehicle.UpdateVehicleInput;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.AlreadyExistsException;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Testes da camada web de cadastro de veículos: contrato JSON, validação do corpo e conversão das exceções em
 * status HTTP.
 */
@WebMvcTest(VehicleController.class)
class VehicleControllerTest {

  private static final UUID VEHICLE_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

  private static final String VALID_BODY = """
      {
        "chassis": "9BWZZZ377LT004251",
        "plate": "ABC1D23",
        "mileage": 45000,
        "brand": "Fiat",
        "model": "Uno",
        "year": 2020,
        "color": "Prata",
        "purchasePrice": 30000,
        "salePrice": 40000.50,
        "origin": "OWNED"
      }
      """;

  private static final String VALID_UPDATE_BODY = """
      {
        "plate": "ABC1D23",
        "mileage": 45000,
        "brand": "Fiat",
        "model": "Uno",
        "year": 2020,
        "color": "Prata",
        "purchasePrice": 30000,
        "salePrice": 40000.50,
        "origin": "OWNED",
        "version": 2
      }
      """;

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

  private static Stream<Arguments> deleteExceptions() {
    return Stream.of(
        Arguments.of(
            new NotFoundException("Veículo não encontrado"),
            HttpStatus.NOT_FOUND,
            "Recurso não encontrado"
        ),
        Arguments.of(
            new ConcurrentUpdateException("Veículo alterado"),
            HttpStatus.CONFLICT,
            "Conflito de atualização"
        ),
        Arguments.of(
            new BusinessRuleException("O veículo já teve venda e não pode ser excluído"),
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Regra de negócio violada"
        )
    );
  }

  private static Stream<Arguments> updateExceptions() {
    return Stream.of(
        Arguments.of(
            new NotFoundException("Veículo não encontrado"),
            HttpStatus.NOT_FOUND,
            "Recurso não encontrado"
        ),
        Arguments.of(
            new ConcurrentUpdateException("Veículo alterado"),
            HttpStatus.CONFLICT,
            "Conflito de atualização"
        ),
        Arguments.of(
            new AlreadyExistsException("Já existe um veículo com a placa ABC1D23"),
            HttpStatus.CONFLICT,
            "Registro duplicado"
        ),
        Arguments.of(
            new BusinessRuleException("O veículo não pode ser editado no status RESERVED"),
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Regra de negócio violada"
        )
    );
  }

  @Test
  void registersVehicle() {
    when(registerVehicle.execute(any())).thenReturn(vehicle("40000.50", VehicleStatus.RECEIVED, 0));

    var response = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_BODY)
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.CREATED)
        .hasHeader("Location", "http://localhost/vehicles/" + VEHICLE_ID)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "id": "0f8fad5b-d9cb-469f-a165-70867728950e",
              "chassis": "9BWZZZ377LT004251",
              "vin": true,
              "vinCheckDigitValid": false,
              "plate": "ABC1D23",
              "mileage": 45000,
              "brand": "Fiat",
              "model": "Uno",
              "year": 2020,
              "color": "Prata",
              "purchasePrice": 30000.00,
              "salePrice": 40000.50,
              "origin": "OWNED",
              "status": "RECEIVED",
              "version": 0
            }
            """);
  }

  @Test
  void rejectsInvalidFieldsWithMessagePerField() {
    var response = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "brand": " ",
              "model": "Uno",
              "year": 2020,
              "color": "Prata",
              "purchasePrice": 30000,
              "salePrice": -1,
              "origin": "OWNED"
            }
            """)
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "status": 400,
              "title": "Requisição inválida",
              "errors": {
                "brand": "A marca é obrigatória",
                "salePrice": "O preço de venda deve ser maior que zero"
              }
            }
            """);
    verifyNoInteractions(registerVehicle);
  }

  @Test
  void rejectsUnknownOrigin() {
    var response = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_BODY.replace("OWNED", "STOLEN"))
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.title")
        .isEqualTo("Requisição inválida");
    verifyNoInteractions(registerVehicle);
  }

  @Test
  void returnsUnprocessableContentForBusinessRuleViolation() {
    when(registerVehicle.execute(any())).thenThrow(new BusinessRuleException("O ano deve estar entre 1900 e 2027"));

    var response = mvc.post().uri("/vehicles")
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_BODY)
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "status": 422,
              "title": "Regra de negócio violada",
              "detail": "O ano deve estar entre 1900 e 2027"
            }
            """);
  }

  @Test
  void getsVehicleById() {
    when(getVehicle.execute(VEHICLE_ID)).thenReturn(vehicle("40000.50", VehicleStatus.AVAILABLE, 3));

    var response = mvc.get().uri("/vehicles/{id}", VEHICLE_ID).exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "id": "0f8fad5b-d9cb-469f-a165-70867728950e",
              "brand": "Fiat",
              "salePrice": 40000.50,
              "status": "AVAILABLE",
              "version": 3
            }
            """);
  }

  @Test
  void returnsNotFoundWhenGettingUnknownVehicle() {
    when(getVehicle.execute(VEHICLE_ID)).thenThrow(new NotFoundException("Veículo não encontrado"));

    var response = mvc.get().uri("/vehicles/{id}", VEHICLE_ID).exchange();

    assertThat(response)
        .hasStatus(HttpStatus.NOT_FOUND)
        .bodyJson()
        .extractingPath("$.title")
        .isEqualTo("Recurso não encontrado");
  }

  @Test
  void updatesVehicleSendingVersionToUseCase() {
    when(updateVehicle.execute(eq(VEHICLE_ID), any())).thenReturn(vehicle("40000.50", VehicleStatus.AVAILABLE, 3));

    var response = mvc.put().uri("/vehicles/{id}", VEHICLE_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_UPDATE_BODY)
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.version")
        .isEqualTo(3);
    verify(updateVehicle).execute(
        VEHICLE_ID,
        new UpdateVehicleInput(
            "ABC1D23",
            45000,
            "Fiat",
            "Uno",
            2020,
            "Prata",
            new BigDecimal("30000"),
            new BigDecimal("40000.50"),
            VehicleOrigin.OWNED,
            2L
        )
    );
  }

  @Test
  void requiresVersionOnUpdate() {
    var response = mvc.put().uri("/vehicles/{id}", VEHICLE_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_BODY)
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.errors.version")
        .isEqualTo("A versão do veículo é obrigatória");
    verifyNoInteractions(updateVehicle);
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("updateExceptions")
  void translatesUpdateExceptionToHttpStatus(
      RuntimeException exception,
      HttpStatus expectedStatus,
      String expectedTitle
  ) {
    when(updateVehicle.execute(eq(VEHICLE_ID), any())).thenThrow(exception);

    var response = mvc.put().uri("/vehicles/{id}", VEHICLE_ID)
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_UPDATE_BODY)
        .exchange();

    assertThat(response)
        .hasStatus(expectedStatus)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "status": %d,
              "title": "%s",
              "detail": "%s"
            }
            """.formatted(expectedStatus.value(), expectedTitle, exception.getMessage()));
  }

  @Test
  void rejectsMalformedId() {
    var response = mvc.put().uri("/vehicles/not-a-uuid")
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_UPDATE_BODY)
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.detail")
        .isEqualTo("Valor inválido para o parâmetro id");
  }

  @Test
  void deletesVehicle() {
    var response = mvc.delete().uri("/vehicles/{id}", VEHICLE_ID).exchange();

    assertThat(response)
        .hasStatus(HttpStatus.NO_CONTENT)
        .body()
        .isEmpty();
    verify(deleteVehicle).execute(VEHICLE_ID);
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource("deleteExceptions")
  void translatesDeleteExceptionToHttpStatus(
      RuntimeException exception,
      HttpStatus expectedStatus,
      String expectedTitle
  ) {
    doThrow(exception).when(deleteVehicle).execute(VEHICLE_ID);

    var response = mvc.delete().uri("/vehicles/{id}", VEHICLE_ID).exchange();

    assertThat(response)
        .hasStatus(expectedStatus)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "status": %d,
              "title": "%s",
              "detail": "%s"
            }
            """.formatted(expectedStatus.value(), expectedTitle, exception.getMessage()));
  }
}

package br.com.fiap.omnidrive.infrastructure.web.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.usecase.CreateSale;
import br.com.fiap.omnidrive.application.usecase.CreateSaleInput;
import br.com.fiap.omnidrive.application.usecase.SaleWithPayment;
import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.PaymentStatus;
import br.com.fiap.omnidrive.domain.sale.Sale;
import java.time.LocalDate;
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
 * Testes da camada web de vendas: contrato JSON, validação do corpo e conversão das exceções em status HTTP.
 */
@WebMvcTest(SaleController.class)
class SaleControllerTest {

  private static final UUID SALE_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

  private static final UUID VEHICLE_ID = UUID.fromString("0f8fad5b-d9cb-469f-a165-70867728950e");

  private static final UUID PAYMENT_ID = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-0e1f2a3b4c5d");

  private static final String PAYMENT_CODE = "3f2b8c1e-5d4a-4b6f-9e7d-1a2b3c4d5e6f";

  private static final String VALID_BODY = """
      {
        "vehicleId": "0f8fad5b-d9cb-469f-a165-70867728950e",
        "buyerCpf": "529.982.247-25",
        "saleDate": "2026-10-01"
      }
      """;

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private CreateSale createSale;

  private static SaleWithPayment result() {
    return new SaleWithPayment(
        Sale.restore(
            SALE_ID,
            VEHICLE_ID,
            new Cpf("52998224725"),
            LocalDate.of(2026, 10, 1),
            Money.of("40000.50")
        ),
        Payment.restore(
            PAYMENT_ID,
            PAYMENT_CODE,
            SALE_ID,
            PaymentStatus.AWAITING_PAYMENT,
            0
        )
    );
  }

  private static Stream<Arguments> createExceptions() {
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
            new InvalidStatusTransitionException("Não é possível passar de RESERVED para RESERVED"),
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Regra de negócio violada"
        ),
        Arguments.of(
            new BusinessRuleException("O CPF é inválido"),
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Regra de negócio violada"
        )
    );
  }

  @Test
  void createsSale() {
    when(createSale.execute(any())).thenReturn(result());

    var response = mvc.post().uri("/sales")
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_BODY)
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.CREATED)
        .hasHeader("Location", "http://localhost/sales/" + SALE_ID)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
              "vehicleId": "0f8fad5b-d9cb-469f-a165-70867728950e",
              "buyerCpf": "52998224725",
              "saleDate": "2026-10-01",
              "amount": 40000.50,
              "paymentCode": "3f2b8c1e-5d4a-4b6f-9e7d-1a2b3c4d5e6f",
              "paymentStatus": "AWAITING_PAYMENT"
            }
            """);
    verify(createSale).execute(
        new CreateSaleInput(
            VEHICLE_ID,
            "529.982.247-25",
            LocalDate.of(2026, 10, 1)
        )
    );
  }

  @Test
  void rejectsMissingFieldsWithMessagePerField() {
    var response = mvc.post().uri("/sales")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "buyerCpf": " "
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
                "vehicleId": "O veículo é obrigatório",
                "buyerCpf": "O CPF do comprador é obrigatório",
                "saleDate": "A data da venda é obrigatória"
              }
            }
            """);
    verifyNoInteractions(createSale);
  }

  @Test
  void rejectsSaleDateInWrongFormat() {
    var response = mvc.post().uri("/sales")
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_BODY.replace("2026-10-01", "01/10/2026"))
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.title")
        .isEqualTo("Requisição inválida");
    verifyNoInteractions(createSale);
  }

  @ParameterizedTest
  @MethodSource("createExceptions")
  void convertsUseCaseExceptionsToHttpStatus(
      RuntimeException exception,
      HttpStatus expectedStatus,
      String expectedTitle
  ) {
    when(createSale.execute(any())).thenThrow(exception);

    var response = mvc.post().uri("/sales")
        .contentType(MediaType.APPLICATION_JSON)
        .content(VALID_BODY)
        .exchange();

    assertThat(response)
        .hasStatus(expectedStatus)
        .bodyJson()
        .extractingPath("$.title")
        .isEqualTo(expectedTitle);
  }
}

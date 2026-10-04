package br.com.fiap.omnidrive.infrastructure.web.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.usecase.ProcessPaymentResult;
import br.com.fiap.omnidrive.application.usecase.ProcessPaymentResultInput;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.PaymentResult;
import br.com.fiap.omnidrive.domain.sale.PaymentStatus;
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
 * Testes da camada web do webhook de pagamento: contrato JSON, validação do corpo e conversão das exceções em status
 * HTTP.
 */
@WebMvcTest(PaymentWebhookController.class)
class PaymentWebhookControllerTest {

  private static final String PAYMENT_CODE = "3f2b8c1e-5d4a-4b6f-9e7d-1a2b3c4d5e6f";

  private static final UUID SALE_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

  private static final String PAID_BODY = """
      {
        "paymentCode": "3f2b8c1e-5d4a-4b6f-9e7d-1a2b3c4d5e6f",
        "status": "PAID"
      }
      """;

  @Autowired
  private MockMvcTester mvc;

  @MockitoBean
  private ProcessPaymentResult processPaymentResult;

  private static Stream<Arguments> processExceptions() {
    return Stream.of(
        Arguments.of(
            new NotFoundException("Pagamento não encontrado: " + PAYMENT_CODE),
            HttpStatus.NOT_FOUND,
            "Recurso não encontrado"
        ),
        Arguments.of(
            new ConcurrentUpdateException("Pagamento alterado"),
            HttpStatus.CONFLICT,
            "Conflito de atualização"
        ),
        Arguments.of(
            new InvalidStatusTransitionException("Não é possível registrar o resultado PAID"),
            HttpStatus.UNPROCESSABLE_CONTENT,
            "Regra de negócio violada"
        )
    );
  }

  @Test
  void registersPaymentResult() {
    when(processPaymentResult.execute(any())).thenReturn(
        Payment.restore(
            UUID.randomUUID(),
            PAYMENT_CODE,
            SALE_ID,
            PaymentStatus.PAID,
            1
        )
    );

    var response = mvc.post().uri("/webhooks/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .content(PAID_BODY)
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .isLenientlyEqualTo("""
            {
              "paymentCode": "3f2b8c1e-5d4a-4b6f-9e7d-1a2b3c4d5e6f",
              "saleId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
              "status": "PAID"
            }
            """);
    verify(processPaymentResult).execute(
        new ProcessPaymentResultInput(
            PAYMENT_CODE,
            PaymentResult.PAID
        )
    );
  }

  @Test
  void rejectsMissingFieldsWithMessagePerField() {
    var response = mvc.post().uri("/webhooks/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {
              "paymentCode": " "
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
                "paymentCode": "O código do pagamento é obrigatório",
                "status": "O status do pagamento é obrigatório"
              }
            }
            """);
    verifyNoInteractions(processPaymentResult);
  }

  @Test
  void rejectsStatusThatProcessorDoesNotSend() {
    var response = mvc.post().uri("/webhooks/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .content(PAID_BODY.replace("PAID", "REFUNDED"))
        .exchange();

    assertThat(response)
        .hasStatus(HttpStatus.BAD_REQUEST)
        .bodyJson()
        .extractingPath("$.title")
        .isEqualTo("Requisição inválida");
    verifyNoInteractions(processPaymentResult);
  }

  @ParameterizedTest
  @MethodSource("processExceptions")
  void convertsUseCaseExceptionsToHttpStatus(
      RuntimeException exception,
      HttpStatus expectedStatus,
      String expectedTitle
  ) {
    when(processPaymentResult.execute(any())).thenThrow(exception);

    var response = mvc.post().uri("/webhooks/payments")
        .contentType(MediaType.APPLICATION_JSON)
        .content(PAID_BODY)
        .exchange();

    assertThat(response)
        .hasStatus(expectedStatus)
        .bodyJson()
        .extractingPath("$.title")
        .isEqualTo(expectedTitle);
  }
}

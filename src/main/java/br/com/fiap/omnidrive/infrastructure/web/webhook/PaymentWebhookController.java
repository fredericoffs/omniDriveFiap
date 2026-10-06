package br.com.fiap.omnidrive.infrastructure.web.webhook;

import br.com.fiap.omnidrive.application.usecase.sale.ProcessPaymentResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook pelo qual o processador de pagamento informa o resultado de um pagamento. A chamada só é aceita com o
 * segredo compartilhado no header {@code X-Webhook-Secret}, conferido pelo {@link WebhookSecretInterceptor}.
 */
@Tag(name = "Webhooks", description = "Notificações enviadas pelo processador de pagamento")
@RestController
@RequestMapping("/webhooks/payments")
public class PaymentWebhookController {

  private final ProcessPaymentResult processPaymentResult;

  PaymentWebhookController(ProcessPaymentResult processPaymentResult) {
    this.processPaymentResult = processPaymentResult;
  }

  @Operation(
      operationId = "receivePaymentResult",
      summary = "Informar resultado do pagamento",
      description = """
          Registra o resultado de um pagamento a partir do código dele. PAID leva o veículo a SOLD (vendido); \
          DECLINED ou CANCELED antes de pagar devolvem o veículo a AVAILABLE (disponível); CANCELED depois de \
          pago estorna o pagamento (REFUNDED) e devolve o veículo a AVAILABLE. Se a venda não for mais a venda \
          atual do veículo, só o pagamento muda. O webhook é idempotente: repetir uma notificação já aplicada \
          responde 200 sem alterar nada. Exige o segredo compartilhado no header X-Webhook-Secret."""
  )
  @SecurityRequirement(name = WebhookSecretInterceptor.SECURITY_SCHEME)
  @ApiResponse(responseCode = "200", description = "Resultado registrado, ou repetição de um resultado já aplicado")
  @ApiResponse(
      responseCode = "400",
      description = "Corpo da requisição inválido, campos ausentes ou status fora de PAID, DECLINED e CANCELED",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "401",
      description = "Header X-Webhook-Secret ausente ou com valor errado",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "Pagamento não encontrado",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "409",
      description = "O pagamento ou o veículo foram alterados por outra operação, como uma notificação simultânea "
          + "igual. A notificação pode ser reenviada",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "O resultado contradiz o status do pagamento, como PAID num pagamento recusado ou cancelado. "
          + "Os estados terminais não são reconciliados",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PostMapping
  PaymentWebhookResponse receive(@Valid @RequestBody PaymentWebhookRequest request) {
    return PaymentWebhookResponse.from(processPaymentResult.execute(request.toInput()));
  }
}

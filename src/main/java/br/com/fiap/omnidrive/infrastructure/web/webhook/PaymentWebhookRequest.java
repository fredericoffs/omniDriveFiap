package br.com.fiap.omnidrive.infrastructure.web.webhook;

import br.com.fiap.omnidrive.application.usecase.ProcessPaymentResultInput;
import br.com.fiap.omnidrive.domain.sale.PaymentResult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo da notificação que o processador de pagamento envia ao webhook.
 */
@Schema(description = "Resultado de um pagamento informado pelo processador de pagamento")
public record PaymentWebhookRequest(

    @Schema(
        description = "Código do pagamento, devolvido na criação da venda (campo paymentCode)",
        example = "3f2b8c1e-5d4a-4b6f-9e7d-1a2b3c4d5e6f"
    )
    @NotBlank(message = "O código do pagamento é obrigatório")
    String paymentCode,

    @Schema(
        description = "Resultado do pagamento: PAID (pago), DECLINED (recusado) ou CANCELED (cancelado). CANCELED "
            + "depois de pago é tratado como estorno",
        example = "PAID"
    )
    @NotNull(message = "O status do pagamento é obrigatório")
    PaymentResult status
) {

  ProcessPaymentResultInput toInput() {
    return new ProcessPaymentResultInput(
        paymentCode,
        status
    );
  }
}

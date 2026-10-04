package br.com.fiap.omnidrive.infrastructure.web.webhook;

import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Resposta do webhook de pagamento, com o status do pagamento depois do registro.
 */
@Schema(description = "Pagamento depois do registro do resultado")
public record PaymentWebhookResponse(

    @Schema(description = "Código do pagamento", example = "3f2b8c1e-5d4a-4b6f-9e7d-1a2b3c4d5e6f")
    String paymentCode,

    @Schema(description = "Identificador da venda do pagamento", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
    UUID saleId,

    @Schema(
        description = "Status do pagamento: AWAITING_PAYMENT (aguardando pagamento), PAID (pago), DECLINED "
            + "(recusado), CANCELED (cancelado) ou REFUNDED (estornado)",
        example = "PAID"
    )
    PaymentStatus status
) {

  static PaymentWebhookResponse from(Payment payment) {
    return new PaymentWebhookResponse(
        payment.getPaymentCode(),
        payment.getSaleId(),
        payment.getStatus()
    );
  }
}

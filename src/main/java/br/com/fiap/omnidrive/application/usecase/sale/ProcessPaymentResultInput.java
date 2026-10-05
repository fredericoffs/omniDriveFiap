package br.com.fiap.omnidrive.application.usecase.sale;

import br.com.fiap.omnidrive.domain.sale.PaymentResult;

/**
 * Resultado de um pagamento informado pelo processador de pagamento.
 *
 * @param paymentCode código do pagamento, devolvido na criação da venda
 * @param result      resultado informado: pago, recusado ou cancelado
 */
public record ProcessPaymentResultInput(
    String paymentCode,
    PaymentResult result
) {
}

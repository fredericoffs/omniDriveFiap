package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.Sale;

/**
 * Venda com o seu pagamento, devolvida ao efetuar e ao consultar uma venda.
 *
 * @param sale    a venda
 * @param payment o pagamento da venda, com o código que o processador de pagamento usa no webhook
 */
public record SaleWithPayment(
    Sale sale,
    Payment payment
) {
}

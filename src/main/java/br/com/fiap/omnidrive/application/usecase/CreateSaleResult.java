package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.Sale;

/**
 * Resultado da venda efetuada.
 *
 * @param sale    a venda gravada
 * @param payment o pagamento aberto para a venda, com o código que o processador de pagamento usa no webhook
 */
public record CreateSaleResult(
    Sale sale,
    Payment payment
) {
}

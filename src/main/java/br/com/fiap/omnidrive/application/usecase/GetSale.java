package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.application.gateway.PaymentGateway;
import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: consultar uma venda pelo id, com o status atual do pagamento. É por ele que o cliente acompanha o
 * resultado que o processador de pagamento informa pelo webhook.
 */
public class GetSale {

  private final SaleGateway saleGateway;
  private final PaymentGateway paymentGateway;

  public GetSale(
      SaleGateway saleGateway,
      PaymentGateway paymentGateway
  ) {
    this.saleGateway = Objects.requireNonNull(saleGateway);
    this.paymentGateway = Objects.requireNonNull(paymentGateway);
  }

  /**
   * @return a venda com o id informado e o seu pagamento
   * @throws NotFoundException se não houver venda com o id informado
   * @throws IllegalStateException se a venda não tiver pagamento, o que não acontece porque os dois são gravados na
   *     mesma transação
   */
  public SaleWithPayment execute(UUID id) {
    Objects.requireNonNull(id);
    var sale = saleGateway.findById(id)
        .orElseThrow(() -> new NotFoundException("Venda não encontrada: " + id));
    var payment = paymentGateway.findBySaleId(id)
        .orElseThrow(() -> new IllegalStateException("Venda sem pagamento: " + id));
    return new SaleWithPayment(sale, payment);
  }
}

package br.com.fiap.omnidrive.application.gateway;

import br.com.fiap.omnidrive.domain.sale.Payment;

/**
 * Persistência de pagamentos, implementada pela camada de infraestrutura. O pagamento tem gateway próprio porque o
 * webhook trabalha só com ele, sem carregar a venda.
 */
public interface PaymentGateway {

  /**
   * Grava o pagamento, novo ou alterado.
   *
   * @return o pagamento como ficou gravado, com a versão nova. Novas alterações devem partir dele, e não do objeto
   *     recebido.
   * @throws br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException se o pagamento foi alterado por outra
   *     operação depois de lido
   */
  Payment save(Payment payment);
}

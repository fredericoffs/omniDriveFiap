package br.com.fiap.omnidrive.application.gateway;

import br.com.fiap.omnidrive.domain.sale.Payment;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistência de pagamentos, implementada pela camada de infraestrutura. O pagamento tem gateway próprio porque o
 * webhook o busca pelo código e o grava com a própria versão, separado da venda.
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

  /**
   * Busca um pagamento pelo código que o processador de pagamento usa no webhook.
   */
  Optional<Payment> findByPaymentCode(String paymentCode);

  /**
   * Busca o pagamento de uma venda. Cada venda tem um único pagamento.
   */
  Optional<Payment> findBySaleId(UUID saleId);
}

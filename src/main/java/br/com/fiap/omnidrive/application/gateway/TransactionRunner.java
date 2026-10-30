package br.com.fiap.omnidrive.application.gateway;

import java.util.function.Supplier;

/**
 * Executa um trecho de um caso de uso numa transação única, implementada pela camada de infraestrutura.
 *
 * <p>Casos de uso que gravam mais de uma entidade, como a venda (venda, pagamento e reserva do veículo), precisam que
 * tudo seja gravado junto ou nada seja. Os casos de uso são Java puro e não podem usar {@code @Transactional}, então
 * deixam a transação explícita por esta interface.
 */
public interface TransactionRunner {

  /**
   * Executa {@code work} numa transação. Se {@code work} lançar uma exceção, tudo o que foi gravado nele é desfeito e
   * a exceção é relançada sem alteração.
   *
   * @return o resultado de {@code work}
   */
  <T> T inTransaction(Supplier<T> work);
}

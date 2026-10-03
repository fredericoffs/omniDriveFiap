package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.application.gateway.TransactionRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Implementação do {@link TransactionRunner} com o {@link TransactionTemplate} do Spring. As gravações dos gateways
 * feitas dentro do trecho participam da mesma transação, e uma exceção não verificada desfaz todas elas.
 */
@Component
public class SpringTransactionRunner implements TransactionRunner {

  private final TransactionTemplate transactionTemplate;

  SpringTransactionRunner(TransactionTemplate transactionTemplate) {
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public <T> T inTransaction(Supplier<T> work) {
    return transactionTemplate.execute(_ -> work.get());
  }
}

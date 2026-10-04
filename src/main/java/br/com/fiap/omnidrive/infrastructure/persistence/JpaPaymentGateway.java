package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.application.gateway.PaymentGateway;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.sale.Payment;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementação do {@link PaymentGateway} com JPA, com controle de concorrência otimista pela coluna {@code version}:
 * a gravação é recusada se a versão do pagamento recebido não for a que está no banco. É isso que impede dois
 * webhooks simultâneos do mesmo pagamento de aplicarem o resultado duas vezes.
 */
@Component
public class JpaPaymentGateway implements PaymentGateway {

  private final PaymentJpaRepository paymentRepository;

  JpaPaymentGateway(PaymentJpaRepository paymentRepository) {
    this.paymentRepository = paymentRepository;
  }

  private static ConcurrentUpdateException concurrentUpdate(Payment payment) {
    return new ConcurrentUpdateException(
        "O pagamento " + payment.getPaymentCode() + " foi alterado por outra operação. Repita a operação"
    );
  }

  @Override
  @Transactional
  public Payment save(Payment payment) {
    var entity = payment.getVersion() == null
        ? new PaymentEntity(payment.getId(), payment.getPaymentCode(), payment.getSaleId())
        : loadWithSameVersion(payment);
    entity.copyFrom(payment);
    try {
      return paymentRepository.saveAndFlush(entity).toDomain();
    } catch (OptimisticLockingFailureException _) {
      throw concurrentUpdate(payment);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Payment> findByPaymentCode(String paymentCode) {
    return paymentRepository.findByPaymentCode(paymentCode).map(PaymentEntity::toDomain);
  }

  private PaymentEntity loadWithSameVersion(Payment payment) {
    var entity = paymentRepository.findById(payment.getId())
        .orElseThrow(() -> concurrentUpdate(payment));
    if (!entity.getVersion().equals(payment.getVersion())) {
      throw concurrentUpdate(payment);
    }
    return entity;
  }
}

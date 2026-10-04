package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapeamento JPA da tabela {@code payment}. Depois de criado, o pagamento só muda de status; as datas de criação e
 * atualização e a versão do controle de concorrência otimista são preenchidas pelo Hibernate.
 */
@Entity
@Table(name = "payment")
class PaymentEntity {

  @Id
  private UUID id;

  @Column(updatable = false)
  private String paymentCode;

  @Column(updatable = false)
  private UUID saleId;

  @Enumerated(EnumType.STRING)
  private PaymentStatus status;

  @CreationTimestamp
  @Column(updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  @Version
  private Long version;

  protected PaymentEntity() {
  }

  PaymentEntity(
      UUID id,
      String paymentCode,
      UUID saleId
  ) {
    this.id = id;
    this.paymentCode = paymentCode;
    this.saleId = saleId;
  }

  /**
   * Copia o status do pagamento do domínio para esta entidade, sem tocar nas datas nem na versão.
   */
  void copyFrom(Payment payment) {
    status = payment.getStatus();
  }

  Long getVersion() {
    return version;
  }

  Payment toDomain() {
    return Payment.restore(
        id,
        paymentCode,
        saleId,
        status,
        version
    );
  }
}

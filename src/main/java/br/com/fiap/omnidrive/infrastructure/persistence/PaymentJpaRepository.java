package br.com.fiap.omnidrive.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repositório Spring Data da tabela {@code payment}.
 */
interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

  Optional<PaymentEntity> findByPaymentCode(String paymentCode);

  Optional<PaymentEntity> findBySaleId(UUID saleId);
}

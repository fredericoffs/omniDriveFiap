package br.com.fiap.omnidrive.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repositório Spring Data da tabela {@code vehicle_status_history}.
 */
interface VehicleStatusHistoryJpaRepository extends JpaRepository<VehicleStatusHistoryEntity, UUID> {
}

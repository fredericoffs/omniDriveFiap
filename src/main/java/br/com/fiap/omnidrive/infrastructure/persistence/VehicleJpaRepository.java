package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório Spring Data da tabela {@code vehicle}.
 */
interface VehicleJpaRepository extends JpaRepository<VehicleEntity, UUID> {

  List<VehicleEntity> findByStatusOrderBySalePriceAsc(VehicleStatus status);
}

package br.com.fiap.omnidrive.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

/**
 * Repositório Spring Data da tabela {@code vehicle_status_history}.
 */
interface VehicleStatusHistoryJpaRepository extends JpaRepository<VehicleStatusHistoryEntity, UUID> {

  @Modifying
  @Query("delete from VehicleStatusHistoryEntity h where h.vehicleId = :vehicleId")
  void deleteByVehicleId(UUID vehicleId);
}

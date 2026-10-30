package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatusHistory;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Mapeamento JPA da tabela {@code vehicle_status_history}. Os registros só são inseridos, nunca alterados.
 */
@Entity
@Table(name = "vehicle_status_history")
class VehicleStatusHistoryEntity {

  @Id
  private UUID id;

  private UUID vehicleId;

  @Enumerated(EnumType.STRING)
  private VehicleStatus previousStatus;

  @Enumerated(EnumType.STRING)
  private VehicleStatus newStatus;

  private String reason;

  private UUID saleId;

  private Instant occurredAt;

  protected VehicleStatusHistoryEntity() {
  }

  static VehicleStatusHistoryEntity from(VehicleStatusHistory history) {
    var entity = new VehicleStatusHistoryEntity();
    entity.id = history.id();
    entity.vehicleId = history.vehicleId();
    entity.previousStatus = history.previousStatus();
    entity.newStatus = history.newStatus();
    entity.reason = history.reason();
    entity.saleId = history.saleId();
    entity.occurredAt = history.occurredAt();
    return entity;
  }
}

package br.com.fiap.omnidrive.domain.vehicle;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de uma mudança de status do veículo. Só é criado pelo próprio {@link Vehicle}.
 *
 * @param id             identificador do registro
 * @param vehicleId      veículo que mudou de status
 * @param previousStatus status anterior, ou {@code null} no cadastro
 * @param newStatus      status novo
 * @param reason         motivo da mudança
 * @param saleId         venda que provocou a mudança, ou {@code null} quando não há venda envolvida
 * @param occurredAt     momento da mudança
 */
public record VehicleStatusHistory(
    UUID id,
    UUID vehicleId,
    VehicleStatus previousStatus,
    VehicleStatus newStatus,
    String reason,
    UUID saleId,
    Instant occurredAt) {

  static VehicleStatusHistory record(
      UUID vehicleId,
      VehicleStatus previousStatus,
      VehicleStatus newStatus,
      String reason,
      UUID saleId
  ) {
    return new VehicleStatusHistory(
        UUID.randomUUID(),
        vehicleId,
        previousStatus,
        newStatus,
        reason,
        saleId,
        Instant.now()
    );
  }
}

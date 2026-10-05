package br.com.fiap.omnidrive.application.usecase.vehicle;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: iniciar a preparação de um veículo recebido (Recebido → Em Preparação).
 */
public class StartVehiclePreparation {

  private final VehicleGateway vehicleGateway;

  public StartVehiclePreparation(VehicleGateway vehicleGateway) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
  }

  /**
   * Coloca o veículo em preparação e persiste a mudança de status.
   *
   * @return o veículo com o novo status
   * @throws NotFoundException se não houver veículo com o id informado
   * @throws InvalidStatusTransitionException se o veículo não estiver Recebido
   * @throws ConcurrentUpdateException se o veículo foi alterado por outra operação durante a mudança
   */
  public Vehicle execute(UUID id) {
    Objects.requireNonNull(id);
    var vehicle = vehicleGateway.findById(id)
        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: " + id));
    vehicle.startPreparation();
    return vehicleGateway.save(vehicle);
  }
}

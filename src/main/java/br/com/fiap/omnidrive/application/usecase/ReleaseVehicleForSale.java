package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: liberar para venda um veículo que terminou a preparação (Em Preparação → Disponível). A partir daí
 * ele aparece na listagem de veículos à venda.
 */
public class ReleaseVehicleForSale {

  private final VehicleGateway vehicleGateway;

  public ReleaseVehicleForSale(VehicleGateway vehicleGateway) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
  }

  /**
   * Libera o veículo para venda e persiste a mudança de status.
   *
   * @return o veículo com o novo status
   * @throws NotFoundException se não houver veículo com o id informado
   * @throws InvalidStatusTransitionException se o veículo não estiver Em Preparação
   * @throws ConcurrentUpdateException se o veículo foi alterado por outra operação durante a mudança
   */
  public Vehicle execute(UUID id) {
    Objects.requireNonNull(id);
    var vehicle = vehicleGateway.findById(id)
        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: " + id));
    vehicle.releaseForSale();
    return vehicleGateway.save(vehicle);
  }
}

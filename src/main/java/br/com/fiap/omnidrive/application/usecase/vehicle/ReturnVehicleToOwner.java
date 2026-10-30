package br.com.fiap.omnidrive.application.usecase.vehicle;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: devolver ao dono um veículo consignado que sai da loja sem ser vendido (Recebido, Em Preparação ou
 * Disponível → Devolvido ao dono).
 */
public class ReturnVehicleToOwner {

  private final VehicleGateway vehicleGateway;

  public ReturnVehicleToOwner(VehicleGateway vehicleGateway) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
  }

  /**
   * Devolve o veículo ao dono e persiste a mudança de status.
   *
   * @return o veículo com status Devolvido ao dono
   * @throws NotFoundException se não houver veículo com o id informado
   * @throws InvalidStatusTransitionException se o veículo não estiver Recebido, Em Preparação nem Disponível
   * @throws BusinessRuleException se o veículo não for consignado
   * @throws ConcurrentUpdateException se o veículo foi alterado por outra operação durante a mudança
   */
  public Vehicle execute(UUID id) {
    Objects.requireNonNull(id);
    var vehicle = vehicleGateway.findById(id)
        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: " + id));
    vehicle.returnToOwner();
    return vehicleGateway.save(vehicle);
  }
}

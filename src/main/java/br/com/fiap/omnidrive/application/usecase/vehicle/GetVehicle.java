package br.com.fiap.omnidrive.application.usecase.vehicle;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: consultar um veículo pelo id. É por ele que o cliente lê a versão atual do veículo antes de editá-lo, inclusive depois de uma edição
 * recusada por conflito.
 */
public class GetVehicle {

  private final VehicleGateway vehicleGateway;

  public GetVehicle(VehicleGateway vehicleGateway) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
  }

  /**
   * @return o veículo com o id informado
   * @throws NotFoundException se não houver veículo com o id informado
   */
  public Vehicle execute(UUID id) {
    Objects.requireNonNull(id);
    return vehicleGateway.findById(id)
        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: " + id));
  }
}

package br.com.fiap.omnidrive.application.usecase.vehicle;

import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: excluir um veículo cadastrado por engano. Só o veículo Recebido que nunca teve venda pode ser excluído,
 * e o histórico de status vai junto.
 */
public class DeleteVehicle {

  private final VehicleGateway vehicleGateway;
  private final SaleGateway saleGateway;

  public DeleteVehicle(
      VehicleGateway vehicleGateway,
      SaleGateway saleGateway
  ) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
    this.saleGateway = Objects.requireNonNull(saleGateway);
  }

  /**
   * Exclui o veículo, se a regra do domínio permitir.
   *
   * @throws NotFoundException se não houver veículo com o id informado
   * @throws BusinessRuleException se o veículo não estiver Recebido ou já tiver tido venda
   * @throws ConcurrentUpdateException se o veículo foi alterado por outra operação durante a exclusão
   */
  public void execute(UUID id) {
    Objects.requireNonNull(id);
    var vehicle = vehicleGateway.findById(id)
        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: " + id));
    vehicle.ensureCanBeDeleted(saleGateway.existsByVehicleId(id));
    vehicleGateway.delete(vehicle);
  }
}

package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: editar os dados de um veículo. Só é permitido enquanto o veículo está Recebido, Em Preparação ou
 * Disponível; a regra fica em {@link Vehicle#update}.
 */
public class UpdateVehicle {

  private final VehicleGateway vehicleGateway;

  public UpdateVehicle(VehicleGateway vehicleGateway) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
  }

  /**
   * Substitui os dados do veículo e persiste a alteração.
   *
   * @return o veículo atualizado
   * @throws NotFoundException se não houver veículo com o id informado
   * @throws br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException se o veículo foi alterado por outra
   *     operação durante a edição
   * @throws br.com.fiap.omnidrive.domain.exception.BusinessRuleException se o status não permitir edição ou algum
   *     dado for inválido
   */
  public Vehicle execute(
      UUID id,
      UpdateVehicleInput input
  ) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(input);
    var vehicle = vehicleGateway.findById(id)
        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: " + id));
    vehicle.update(
        input.brand(),
        input.model(),
        input.year(),
        input.color(),
        Money.ofNullable(input.purchasePrice()),
        Money.ofNullable(input.salePrice()),
        input.origin()
    );
    return vehicleGateway.save(vehicle);
  }
}

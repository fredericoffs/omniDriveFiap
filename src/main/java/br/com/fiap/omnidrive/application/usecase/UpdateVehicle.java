package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: editar os dados de um veículo. Só é permitido enquanto o veículo está Recebido, Em Preparação ou
 * Disponível; a regra fica em {@link Vehicle#update}.
 *
 * <p>O cliente informa a versão que leu. Se o veículo mudou desde então, a edição é recusada, para que uma
 * alteração feita por outra pessoa não seja sobrescrita sem aviso.
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
   * @throws ConcurrentUpdateException se o veículo foi alterado depois que o cliente o leu
   * @throws BusinessRuleException se a versão não for informada, o status não permitir edição ou algum dado for
   *     inválido
   */
  public Vehicle execute(
      UUID id,
      UpdateVehicleInput input
  ) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(input);
    var vehicle = vehicleGateway.findById(id)
        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: " + id));
    if (input.version() == null) {
      throw new BusinessRuleException("A versão do veículo é obrigatória");
    }
    if (!input.version().equals(vehicle.getVersion())) {
      throw new ConcurrentUpdateException(
          "O veículo " + id + " foi alterado por outra operação. Consulte-o novamente e repita a operação"
      );
    }
    vehicle.update(new VehicleDetails(
        input.brand(),
        input.model(),
        input.year(),
        input.color(),
        Money.ofNullable(input.purchasePrice()),
        Money.ofNullable(input.salePrice()),
        input.origin()
    ));
    return vehicleGateway.save(vehicle);
  }
}

package br.com.fiap.omnidrive.application.usecase.vehicle;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.AlreadyExistsException;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: editar os dados de um veículo. Só é permitido enquanto o veículo está Recebido, Em Preparação ou
 * Disponível; a regra fica em {@link Vehicle#update}.
 *
 * <p>O chassi não é editável. A placa pode mudar (ex.: conversão para o padrão Mercosul), desde que não pertença a
 * outro veículo.
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
   * @throws AlreadyExistsException se a nova placa pertencer a outro veículo
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
    var previousPlate = vehicle.getPlate();
    vehicle.update(new VehicleDetails(
        input.brand(),
        input.model(),
        input.year(),
        input.color(),
        new Plate(input.plate()),
        input.mileage(),
        Money.ofNullable(input.purchasePrice()),
        Money.ofNullable(input.salePrice()),
        input.origin()
    ));
    if (!vehicle.getPlate().equals(previousPlate) && vehicleGateway.existsByPlate(vehicle.getPlate())) {
      throw new AlreadyExistsException("Já existe um veículo com a placa " + vehicle.getPlate().value());
    }
    return vehicleGateway.save(vehicle);
  }
}

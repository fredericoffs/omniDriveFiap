package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;

import java.util.Objects;

/**
 * Caso de uso: cadastrar um veículo que chegou à loja. O veículo entra como Recebido e o cadastro gera o primeiro
 * registro do histórico de status.
 */
public class RegisterVehicle {

  private final VehicleGateway vehicleGateway;

  public RegisterVehicle(VehicleGateway vehicleGateway) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
  }

  /**
   * Cadastra e persiste o veículo.
   *
   * @return o veículo cadastrado
   * @throws br.com.fiap.omnidrive.domain.exception.BusinessRuleException se algum dado for inválido
   */
  public Vehicle execute(RegisterVehicleInput input) {
    Objects.requireNonNull(input);
    var vehicle = Vehicle.register(new VehicleDetails(
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

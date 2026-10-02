package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.math.BigDecimal;
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

  private static Money toMoney(BigDecimal amount) {
    return amount == null ? null : new Money(amount);
  }

  /**
   * Cadastra e persiste o veículo.
   *
   * @return o veículo cadastrado
   * @throws br.com.fiap.omnidrive.domain.exception.BusinessRuleException se algum dado for inválido
   */
  public Vehicle execute(RegisterVehicleInput input) {
    Objects.requireNonNull(input);
    var vehicle = Vehicle.register(
        input.brand(),
        input.model(),
        input.year(),
        input.color(),
        toMoney(input.purchasePrice()),
        toMoney(input.salePrice()),
        input.origin()
    );
    vehicleGateway.save(vehicle);
    return vehicle;
  }
}

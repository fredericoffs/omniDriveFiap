package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.AlreadyExistsException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;

import java.util.Objects;

/**
 * Caso de uso: cadastrar um veículo que chegou à loja. O veículo entra como Recebido e o cadastro gera o primeiro
 * registro do histórico de status.
 *
 * <p>O chassi e a placa são únicos: um veículo que já passou pela loja volta pela reentrada, não por um novo
 * cadastro.
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
   * @throws AlreadyExistsException se já houver um veículo com o mesmo chassi ou a mesma placa
   */
  public Vehicle execute(RegisterVehicleInput input) {
    Objects.requireNonNull(input);
    var vehicle = Vehicle.register(
        new Chassis(input.chassis()),
        new VehicleDetails(
            input.brand(),
            input.model(),
            input.year(),
            input.color(),
            new Plate(input.plate()),
            input.mileage(),
            Money.ofNullable(input.purchasePrice()),
            Money.ofNullable(input.salePrice()),
            input.origin()
        )
    );
    if (vehicleGateway.existsByChassis(vehicle.getChassis())) {
      throw new AlreadyExistsException("Já existe um veículo com o chassi " + vehicle.getChassis().value());
    }
    if (vehicleGateway.existsByPlate(vehicle.getPlate())) {
      throw new AlreadyExistsException("Já existe um veículo com a placa " + vehicle.getPlate().value());
    }
    return vehicleGateway.save(vehicle);
  }
}

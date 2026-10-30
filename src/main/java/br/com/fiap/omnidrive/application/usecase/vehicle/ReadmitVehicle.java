package br.com.fiap.omnidrive.application.usecase.vehicle;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: reentrada de um veículo vendido ou devolvido ao dono que voltou à loja (Vendido ou Devolvido ao dono →
 * Recebido). O cadastro é o mesmo, com um novo valor de entrada e a quilometragem atual, que deve ser maior que a
 * registrada (igual só é aceita vindo de Devolvido ao dono); os valores anteriores ficam registrados no histórico de
 * status.
 */
public class ReadmitVehicle {

  private final VehicleGateway vehicleGateway;

  public ReadmitVehicle(VehicleGateway vehicleGateway) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
  }

  /**
   * Registra a volta do veículo à loja e persiste a mudança de status.
   *
   * @return o veículo com status Recebido e o novo valor de entrada
   * @throws NotFoundException se não houver veículo com o id informado
   * @throws InvalidStatusTransitionException se o veículo não estiver Vendido nem Devolvido ao dono
   * @throws BusinessRuleException se o novo valor de entrada ou a quilometragem não forem informados ou forem
   *     inválidos
   * @throws ConcurrentUpdateException se o veículo foi alterado por outra operação durante a mudança
   */
  public Vehicle execute(
      UUID id,
      ReadmitVehicleInput input
  ) {
    Objects.requireNonNull(id);
    Objects.requireNonNull(input);
    var vehicle = vehicleGateway.findById(id)
        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: " + id));
    vehicle.readmit(
        Money.ofNullable(input.purchasePrice()),
        input.mileage()
    );
    return vehicleGateway.save(vehicle);
  }
}

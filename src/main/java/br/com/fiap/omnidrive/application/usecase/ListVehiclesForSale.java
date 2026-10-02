package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;

import java.util.List;
import java.util.Objects;

/**
 * Caso de uso: listar os veículos à venda. Só entram veículos Disponíveis, ordenados pelo preço de venda, do menor
 * para o maior.
 */
public class ListVehiclesForSale {

  private final VehicleGateway vehicleGateway;

  public ListVehiclesForSale(VehicleGateway vehicleGateway) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
  }

  /**
   * @return os veículos à venda, em ordem crescente de preço
   */
  public List<Vehicle> execute() {
    return vehicleGateway.findByStatusOrderBySalePrice(VehicleStatus.AVAILABLE);
  }
}

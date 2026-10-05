package br.com.fiap.omnidrive.application.usecase.vehicle;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.pagination.PageQuery;
import br.com.fiap.omnidrive.application.pagination.PageResult;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;

import java.util.Objects;

/**
 * Caso de uso: listar os veículos à venda. Só entram veículos Disponíveis, ordenados pelo preço de venda, do menor
 * para o maior, com desempate pelo id.
 */
public class ListVehiclesForSale {

  private final VehicleGateway vehicleGateway;

  public ListVehiclesForSale(VehicleGateway vehicleGateway) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
  }

  /**
   * @return a página pedida dos veículos à venda, em ordem crescente de preço
   */
  public PageResult<Vehicle> execute(PageQuery pageQuery) {
    return vehicleGateway.findByStatusOrderBySalePrice(
        VehicleStatus.AVAILABLE,
        Objects.requireNonNull(pageQuery)
    );
  }
}

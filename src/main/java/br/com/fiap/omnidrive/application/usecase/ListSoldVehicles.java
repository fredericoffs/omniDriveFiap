package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.pagination.PageQuery;
import br.com.fiap.omnidrive.application.pagination.PageResult;
import br.com.fiap.omnidrive.domain.sale.Sale;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: listar os veículos vendidos. Só entram veículos Vendidos, cada um com a venda atual, ordenados pelo
 * valor da venda, do menor para o maior, com desempate pelo id do veículo.
 *
 * <p>Cada veículo aparece no máximo uma vez: depois da reentrada, ele sai da listagem, e a venda anterior continua
 * registrada no histórico.
 */
public class ListSoldVehicles {

  private final VehicleGateway vehicleGateway;
  private final SaleGateway saleGateway;

  public ListSoldVehicles(
      VehicleGateway vehicleGateway,
      SaleGateway saleGateway
  ) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
    this.saleGateway = Objects.requireNonNull(saleGateway);
  }

  /**
   * @return a página pedida dos veículos vendidos com a venda atual, em ordem crescente do valor da venda
   */
  public PageResult<SoldVehicle> execute(PageQuery pageQuery) {
    var vehicles = vehicleGateway.findSoldOrderBySaleAmount(Objects.requireNonNull(pageQuery));
    var saleIds = vehicles.content().stream()
        .map(Vehicle::getCurrentSaleId)
        .toList();
    Map<UUID, Sale> salesById = saleGateway.findAllById(saleIds).stream()
        .collect(Collectors.toMap(Sale::getId, Function.identity()));
    return vehicles.map(vehicle -> new SoldVehicle(vehicle, salesById.get(vehicle.getCurrentSaleId())));
  }
}

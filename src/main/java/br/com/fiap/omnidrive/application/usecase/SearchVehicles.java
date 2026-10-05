package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.application.gateway.VehicleFilter;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.pagination.PageQuery;
import br.com.fiap.omnidrive.application.pagination.PageResult;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.util.Objects;

/**
 * Caso de uso: buscar veículos em qualquer status, por placa, chassi, status, marca e modelo. É por ele que o
 * cliente encontra o veículo para consultar, editar ou mudar o status, inclusive os que não aparecem nas listagens à
 * venda e de vendidos.
 *
 * <p>A placa e o chassi passam pela mesma normalização do cadastro e são comparados pelo valor exato. A marca e o
 * modelo são comparados por trecho, sem diferenciar maiúsculas de minúsculas. O resultado vem do cadastro mais
 * recente para o mais antigo, com desempate pelo id.
 */
public class SearchVehicles {

  private final VehicleGateway vehicleGateway;

  public SearchVehicles(VehicleGateway vehicleGateway) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
  }

  private static String textOrNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  /**
   * @return a página pedida dos veículos que atendem a todos os filtros informados
   * @throws br.com.fiap.omnidrive.domain.exception.BusinessRuleException se a placa ou o chassi tiverem formato
   *                                                                      inválido
   */
  public PageResult<Vehicle> execute(
      SearchVehiclesInput input,
      PageQuery pageQuery
  ) {
    Objects.requireNonNull(input);
    Objects.requireNonNull(pageQuery);
    var plate = textOrNull(input.plate());
    var chassis = textOrNull(input.chassis());
    var filter = new VehicleFilter(
        plate == null ? null : new Plate(plate),
        chassis == null ? null : new Chassis(chassis),
        input.statuses(),
        textOrNull(input.brand()),
        textOrNull(input.model())
    );
    return vehicleGateway.search(filter, pageQuery);
  }
}

package br.com.fiap.omnidrive.application.gateway;

import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;

import java.util.Set;

/**
 * Filtros da busca de veículos. Um filtro nulo, ou o conjunto de status vazio, não restringe a busca. Os filtros
 * informados se combinam: o veículo precisa atender a todos.
 *
 * @param plate    placa, comparada pelo valor exato
 * @param chassis  chassi, comparado pelo valor exato
 * @param statuses status aceitos; vazio aceita todos
 * @param brand    trecho da marca, sem diferenciar maiúsculas de minúsculas
 * @param model    trecho do modelo, sem diferenciar maiúsculas de minúsculas
 */
public record VehicleFilter(
    Plate plate,
    Chassis chassis,
    Set<VehicleStatus> statuses,
    String brand,
    String model
) {

  public VehicleFilter {
    statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
  }
}

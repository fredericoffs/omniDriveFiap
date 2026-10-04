package br.com.fiap.omnidrive.application.gateway;

import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência de veículos, implementada pela camada de infraestrutura.
 */
public interface VehicleGateway {

  /**
   * Grava o veículo e as mudanças de status ainda não persistidas ({@link Vehicle#getStatusChanges()}) na mesma transação.
   *
   * @return o veículo como ficou gravado, com a versão nova e sem mudanças de status pendentes. Novas alterações devem partir dele, e não do objeto
   * recebido.
   * @throws br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException se o veículo foi alterado por outra operação depois de lido
   * @throws br.com.fiap.omnidrive.domain.exception.AlreadyExistsException    se outro veículo já tiver o mesmo chassi ou a mesma placa
   */
  Vehicle save(Vehicle vehicle);

  /**
   * Indica se há um veículo com o chassi informado.
   */
  boolean existsByChassis(Chassis chassis);

  /**
   * Indica se há um veículo com a placa informada.
   */
  boolean existsByPlate(Plate plate);

  /**
   * Busca um veículo pelo id.
   */
  Optional<Vehicle> findById(UUID id);

  /**
   * Lista os veículos com o status informado, ordenados pelo preço de venda, do menor para o maior.
   */
  List<Vehicle> findByStatusOrderBySalePrice(VehicleStatus status);

  /**
   * Lista os veículos Vendidos, ordenados pelo valor da venda atual, do menor para o maior.
   */
  List<Vehicle> findSoldOrderBySaleAmount();
}

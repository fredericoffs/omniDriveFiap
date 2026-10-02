package br.com.fiap.omnidrive.application.gateway;

import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistência de veículos, implementada pela camada de infraestrutura.
 */
public interface VehicleGateway {

  /**
   * Grava o veículo e as mudanças de status ainda não persistidas ({@link Vehicle#getStatusChanges()}) na mesma transação.
   */
  void save(Vehicle vehicle);

  /**
   * Busca um veículo pelo id.
   */
  Optional<Vehicle> findById(UUID id);
}

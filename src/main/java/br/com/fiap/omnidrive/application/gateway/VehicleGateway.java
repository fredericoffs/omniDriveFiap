package br.com.fiap.omnidrive.application.gateway;

import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

/**
 * Persistência de veículos, implementada pela camada de infraestrutura.
 */
public interface VehicleGateway {

  /**
   * Grava o veículo e as mudanças de status ainda não persistidas ({@link Vehicle#getStatusChanges()}) na mesma transação.
   */
  void save(Vehicle vehicle);
}

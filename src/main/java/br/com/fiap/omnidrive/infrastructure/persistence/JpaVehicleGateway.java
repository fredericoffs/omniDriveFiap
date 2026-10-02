package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do {@link VehicleGateway} com JPA. O veículo e o histórico de status são gravados na mesma
 * transação.
 */
@Component
public class JpaVehicleGateway implements VehicleGateway {

  private final VehicleJpaRepository vehicleRepository;
  private final VehicleStatusHistoryJpaRepository statusHistoryRepository;

  JpaVehicleGateway(
      VehicleJpaRepository vehicleRepository,
      VehicleStatusHistoryJpaRepository statusHistoryRepository
  ) {
    this.vehicleRepository = vehicleRepository;
    this.statusHistoryRepository = statusHistoryRepository;
  }

  @Override
  @Transactional
  public void save(Vehicle vehicle) {
    var entity = vehicleRepository.findById(vehicle.getId())
        .orElseGet(() -> new VehicleEntity(vehicle.getId()));
    entity.copyFrom(vehicle);
    vehicleRepository.save(entity);
    statusHistoryRepository.saveAll(
        vehicle.getStatusChanges().stream()
            .map(VehicleStatusHistoryEntity::from)
            .toList()
    );
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Vehicle> findById(UUID id) {
    return vehicleRepository.findById(id).map(VehicleEntity::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Vehicle> findByStatusOrderBySalePrice(VehicleStatus status) {
    return vehicleRepository.findByStatusOrderBySalePriceAsc(status).stream()
        .map(VehicleEntity::toDomain)
        .toList();
  }
}

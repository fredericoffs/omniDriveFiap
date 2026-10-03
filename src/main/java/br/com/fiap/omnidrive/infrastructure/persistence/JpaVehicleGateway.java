package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do {@link VehicleGateway} com JPA. O veículo e o histórico de status são gravados na mesma
 * transação, com controle de concorrência otimista pela coluna {@code version}: a gravação é recusada se a versão
 * do veículo recebido não for a que está no banco.
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

  private static ConcurrentUpdateException concurrentUpdate(Vehicle vehicle) {
    return new ConcurrentUpdateException(
        "O veículo " + vehicle.getId() + " foi alterado por outra operação. Consulte-o novamente e repita a operação"
    );
  }

  @Override
  @Transactional
  public Vehicle save(Vehicle vehicle) {
    var entity = vehicle.getVersion() == null
        ? new VehicleEntity(vehicle.getId())
        : loadWithSameVersion(vehicle);
    entity.copyFrom(vehicle);
    try {
      entity = vehicleRepository.saveAndFlush(entity);
    } catch (OptimisticLockingFailureException _) {
      throw concurrentUpdate(vehicle);
    }
    statusHistoryRepository.saveAll(
        vehicle.getStatusChanges().stream()
            .map(VehicleStatusHistoryEntity::from)
            .toList()
    );
    return entity.toDomain();
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

  private VehicleEntity loadWithSameVersion(Vehicle vehicle) {
    var entity = vehicleRepository.findById(vehicle.getId())
        .orElseThrow(() -> concurrentUpdate(vehicle));
    if (!entity.getVersion().equals(vehicle.getVersion())) {
      throw concurrentUpdate(vehicle);
    }
    return entity;
  }
}

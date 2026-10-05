package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.pagination.PageQuery;
import br.com.fiap.omnidrive.application.pagination.PageResult;
import br.com.fiap.omnidrive.domain.exception.AlreadyExistsException;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementação do {@link VehicleGateway} com JPA. O veículo e o histórico de status são gravados na mesma
 * transação, com controle de concorrência otimista pela coluna {@code version}: a gravação é recusada se a versão
 * do veículo recebido não for a que está no banco.
 *
 * <p>O caso de uso confere o chassi e a placa antes de gravar, mas dois cadastros simultâneos podem passar pela
 * conferência. Nesse caso, a restrição única do banco recusa o segundo, e a violação vira
 * {@link AlreadyExistsException}.
 */
@Component
public class JpaVehicleGateway implements VehicleGateway {

  private static final String CHASSIS_CONSTRAINT = "uk_vehicle_chassis";
  private static final String PLATE_CONSTRAINT = "uk_vehicle_plate";

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

  private static RuntimeException alreadyExists(
      Vehicle vehicle,
      DataIntegrityViolationException exception
  ) {
    var constraint = exception.getCause() instanceof ConstraintViolationException violation
        ? violation.getConstraintName()
        : null;
    if (CHASSIS_CONSTRAINT.equals(constraint)) {
      return new AlreadyExistsException("Já existe um veículo com o chassi " + vehicle.getChassis().value());
    }
    if (PLATE_CONSTRAINT.equals(constraint)) {
      return new AlreadyExistsException("Já existe um veículo com a placa " + vehicle.getPlate().value());
    }
    return exception;
  }

  @Override
  @Transactional
  public Vehicle save(Vehicle vehicle) {
    var entity = vehicle.getVersion() == null
        ? new VehicleEntity(vehicle.getId(), vehicle.getChassis())
        : loadWithSameVersion(vehicle);
    entity.copyFrom(vehicle);
    try {
      entity = vehicleRepository.saveAndFlush(entity);
    } catch (OptimisticLockingFailureException _) {
      throw concurrentUpdate(vehicle);
    } catch (DataIntegrityViolationException exception) {
      throw alreadyExists(vehicle, exception);
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
  public boolean existsByChassis(Chassis chassis) {
    return vehicleRepository.existsByChassis(chassis.value());
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByPlate(Plate plate) {
    return vehicleRepository.existsByPlate(plate.value());
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Vehicle> findById(UUID id) {
    return vehicleRepository.findById(id).map(VehicleEntity::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<Vehicle> findByStatusOrderBySalePrice(
      VehicleStatus status,
      PageQuery pageQuery
  ) {
    var pageable = PageConversion.toPageable(pageQuery);
    return PageConversion.toResult(
        vehicleRepository.findByStatusOrderBySalePriceAscIdAsc(status, pageable),
        VehicleEntity::toDomain
    );
  }

  @Override
  @Transactional(readOnly = true)
  public PageResult<Vehicle> findSoldOrderBySaleAmount(PageQuery pageQuery) {
    var pageable = PageConversion.toPageable(pageQuery);
    return PageConversion.toResult(
        vehicleRepository.findByStatusOrderByCurrentSaleAmount(VehicleStatus.SOLD, pageable),
        VehicleEntity::toDomain
    );
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

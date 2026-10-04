package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositório Spring Data da tabela {@code vehicle}.
 */
interface VehicleJpaRepository extends JpaRepository<VehicleEntity, UUID> {

  List<VehicleEntity> findByStatusOrderBySalePriceAsc(VehicleStatus status);

  @Query("""
      select v from VehicleEntity v
      join SaleEntity s on s.id = v.currentSaleId
      where v.status = :status
      order by s.amount""")
  List<VehicleEntity> findByStatusOrderByCurrentSaleAmount(VehicleStatus status);

  boolean existsByChassis(String chassis);

  boolean existsByPlate(String plate);
}

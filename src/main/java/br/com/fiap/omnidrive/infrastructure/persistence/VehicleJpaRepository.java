package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Repositório Spring Data da tabela {@code vehicle}.
 */
interface VehicleJpaRepository extends JpaRepository<VehicleEntity, UUID> {

  Page<VehicleEntity> findByStatusOrderBySalePriceAscIdAsc(
      VehicleStatus status,
      Pageable pageable
  );

  @Query(
      value = """
          select v from VehicleEntity v
          join SaleEntity s on s.id = v.currentSaleId
          where v.status = :status
          order by s.amount, v.id""",
      countQuery = """
          select count(v) from VehicleEntity v
          join SaleEntity s on s.id = v.currentSaleId
          where v.status = :status"""
  )
  Page<VehicleEntity> findByStatusOrderByCurrentSaleAmount(
      VehicleStatus status,
      Pageable pageable
  );

  boolean existsByChassis(String chassis);

  boolean existsByPlate(String plate);
}

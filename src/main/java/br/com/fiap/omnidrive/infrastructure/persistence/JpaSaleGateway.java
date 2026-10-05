package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.domain.sale.Sale;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementação do {@link SaleGateway} com JPA. A venda é só inserida, e a gravação entra na transação em andamento quando houver uma, como a do caso
 * de uso efetuar venda.
 */
@Component
public class JpaSaleGateway implements SaleGateway {

  private final SaleJpaRepository saleRepository;

  JpaSaleGateway(SaleJpaRepository saleRepository) {
    this.saleRepository = saleRepository;
  }

  @Override
  @Transactional
  public Sale save(Sale sale) {
    return saleRepository.save(SaleEntity.from(sale)).toDomain();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Sale> findById(UUID id) {
    return saleRepository.findById(id).map(SaleEntity::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<Sale> findAllById(Collection<UUID> ids) {
    return saleRepository.findAllById(ids).stream()
        .map(SaleEntity::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsByVehicleId(UUID vehicleId) {
    return saleRepository.existsByVehicleId(vehicleId);
  }
}

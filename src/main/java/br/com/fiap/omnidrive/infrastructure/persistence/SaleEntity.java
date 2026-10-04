package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.sale.Sale;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Mapeamento JPA da tabela {@code sale}. A venda não muda depois de criada, então a entidade é só inserida; a data de
 * criação é preenchida pelo Hibernate.
 */
@Entity
@Immutable
@Table(name = "sale")
class SaleEntity {

  @Id
  private UUID id;

  private UUID vehicleId;

  private String buyerCpf;

  private LocalDate saleDate;

  private BigDecimal amount;

  @CreationTimestamp
  private Instant createdAt;

  protected SaleEntity() {
  }

  static SaleEntity from(Sale sale) {
    var entity = new SaleEntity();
    entity.id = sale.getId();
    entity.vehicleId = sale.getVehicleId();
    entity.buyerCpf = sale.getBuyerCpf().number();
    entity.saleDate = sale.getSaleDate();
    entity.amount = sale.getAmount().amount();
    return entity;
  }

  Sale toDomain() {
    return Sale.restore(
        id,
        vehicleId,
        new Cpf(buyerCpf),
        saleDate,
        new Money(amount)
    );
  }
}

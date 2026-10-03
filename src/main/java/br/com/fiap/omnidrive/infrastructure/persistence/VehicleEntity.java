package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Mapeamento JPA da tabela {@code vehicle}. Converte de e para o {@link Vehicle} do domínio; as datas de criação e
 * atualização e a versão do controle de concorrência otimista são preenchidas pelo Hibernate.
 */
@Entity
@Table(name = "vehicle")
class VehicleEntity {

  @Id
  private UUID id;

  private String brand;

  private String model;

  private short year;

  private String color;

  private BigDecimal purchasePrice;

  private BigDecimal salePrice;

  @Enumerated(EnumType.STRING)
  private VehicleStatus status;

  @Enumerated(EnumType.STRING)
  private VehicleOrigin origin;

  private UUID currentSaleId;

  @CreationTimestamp
  @Column(updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;

  @Version
  private Long version;

  protected VehicleEntity() {
  }

  VehicleEntity(UUID id) {
    this.id = id;
  }

  /**
   * Copia o estado do veículo do domínio para esta entidade, sem tocar nas datas de criação e atualização nem na
   * versão.
   */
  void copyFrom(Vehicle vehicle) {
    brand = vehicle.getBrand();
    model = vehicle.getModel();
    year = (short) vehicle.getYear();
    color = vehicle.getColor();
    purchasePrice = vehicle.getPurchasePrice().amount();
    salePrice = vehicle.getSalePrice().amount();
    status = vehicle.getStatus();
    origin = vehicle.getOrigin();
    currentSaleId = vehicle.getCurrentSaleId();
  }

  Long getVersion() {
    return version;
  }

  Vehicle toDomain() {
    var details = new VehicleDetails(
        brand,
        model,
        year,
        color,
        new Money(purchasePrice),
        new Money(salePrice),
        origin
    );
    return Vehicle.restore(
        id,
        details,
        status,
        currentSaleId,
        version
    );
  }
}

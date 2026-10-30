package br.com.fiap.omnidrive.domain.sale;

import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

/**
 * Venda de um veículo para um comprador. O valor é copiado do preço de venda do veículo no momento da venda, então
 * edições posteriores do preço não mudam a venda.
 *
 * <p>A venda não tem status próprio: o andamento vem do {@link Payment} dela e da venda atual do veículo. Um veículo
 * pode ter várias vendas ao longo do tempo, uma por ciclo na loja.
 */
public class Sale {

  private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

  private final UUID id;
  private final UUID vehicleId;
  private final Cpf buyerCpf;
  private final LocalDate saleDate;
  private final Money amount;

  private Sale(
      UUID id,
      UUID vehicleId,
      Cpf buyerCpf,
      LocalDate saleDate,
      Money amount
  ) {
    this.id = Objects.requireNonNull(id);
    this.vehicleId = Objects.requireNonNull(vehicleId);
    this.buyerCpf = Objects.requireNonNull(buyerCpf);
    this.saleDate = Objects.requireNonNull(saleDate);
    this.amount = Objects.requireNonNull(amount);
  }

  /**
   * Cria a venda de um veículo, copiando o preço de venda dele. Não reserva o veículo: isso é feito pelo
   * {@link Vehicle#reserve(UUID)}, na mesma operação.
   *
   * @throws BusinessRuleException se o CPF ou a data faltarem, ou se a data estiver no futuro
   */
  public static Sale create(
      Vehicle vehicle,
      Cpf buyerCpf,
      LocalDate saleDate
  ) {
    if (buyerCpf == null) {
      throw new BusinessRuleException("O CPF do comprador é obrigatório");
    }
    if (saleDate == null) {
      throw new BusinessRuleException("A data da venda é obrigatória");
    }
    if (saleDate.isAfter(LocalDate.now(ZONE))) {
      throw new BusinessRuleException("A data da venda não pode estar no futuro");
    }
    return new Sale(
        UUID.randomUUID(),
        vehicle.getId(),
        buyerCpf,
        saleDate,
        vehicle.getSalePrice()
    );
  }

  /**
   * Reconstrói uma venda já existente. Uso exclusivo da persistência.
   */
  public static Sale restore(
      UUID id,
      UUID vehicleId,
      Cpf buyerCpf,
      LocalDate saleDate,
      Money amount
  ) {
    return new Sale(id, vehicleId, buyerCpf, saleDate, amount);
  }

  public UUID getId() {
    return id;
  }

  public UUID getVehicleId() {
    return vehicleId;
  }

  public Cpf getBuyerCpf() {
    return buyerCpf;
  }

  public LocalDate getSaleDate() {
    return saleDate;
  }

  /**
   * Valor da venda fechada, usado na ordenação da listagem de vendidos.
   */
  public Money getAmount() {
    return amount;
  }
}

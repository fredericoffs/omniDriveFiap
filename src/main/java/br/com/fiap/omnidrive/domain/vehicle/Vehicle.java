package br.com.fiap.omnidrive.domain.vehicle;

import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Veículo da loja e as regras do seu ciclo de status.
 *
 * <p>O cadastro é único: um veículo vendido pode voltar à loja pela reentrada e ser vendido de novo. Toda mudança
 * de status gera um {@link VehicleStatusHistory}, disponível em {@link #getStatusChanges()} até ser persistido.
 *
 * <p>A venda atual ({@link #getCurrentSaleId()}) é a venda que controla o veículo enquanto ele está Reservado ou
 * Vendido. As transições ligadas a uma venda só são aceitas para ela, e o pagamento de uma venda antiga não muda mais o
 * veículo.
 *
 * <p>A versão ({@link #getVersion()}) serve ao controle de concorrência otimista: a persistência recusa a gravação
 * se o veículo foi alterado por outra operação depois de lido.
 *
 * <p>Transições permitidas:
 * <ul>
 *     <li>Recebido → Em Preparação ({@link #startPreparation()})</li>
 *     <li>Em Preparação → Disponível ({@link #releaseForSale()})</li>
 *     <li>Disponível → Reservado ({@link #reserve(UUID)})</li>
 *     <li>Reservado → Vendido ({@link #confirmSale(UUID)})</li>
 *     <li>Reservado → Disponível ({@link #releaseReservation(UUID)})</li>
 *     <li>Vendido → Disponível ({@link #reverseSale(UUID)})</li>
 *     <li>Vendido → Recebido ({@link #readmit(Money)})</li>
 * </ul>
 */
public class Vehicle {

  private final UUID id;
  private final Long version;
  private final List<VehicleStatusHistory> statusChanges = new ArrayList<>();
  private VehicleDetails details;
  private VehicleStatus status;
  private UUID currentSaleId;

  private Vehicle(
      UUID id,
      VehicleDetails details,
      VehicleStatus status,
      UUID currentSaleId,
      Long version
  ) {
    this.id = Objects.requireNonNull(id);
    this.details = Objects.requireNonNull(details);
    this.status = status;
    this.currentSaleId = currentSaleId;
    this.version = version;
  }

  /**
   * Cadastra um veículo que chegou à loja. Ele entra como Recebido.
   */
  public static Vehicle register(VehicleDetails details) {
    var vehicle = new Vehicle(UUID.randomUUID(), details, null, null, null);
    vehicle.changeStatus(VehicleStatus.RECEIVED, "Cadastro do veículo", null);
    return vehicle;
  }

  /**
   * Reconstrói um veículo já existente, sem gerar histórico. Uso exclusivo da persistência.
   *
   * @param currentSaleId venda atual, obrigatória em Reservado e Vendido e ausente nos demais status
   * @param version       versão lida do banco, usada no controle de concorrência otimista
   * @throws IllegalArgumentException se a venda atual não combinar com o status
   */
  public static Vehicle restore(
      UUID id,
      VehicleDetails details,
      VehicleStatus status,
      UUID currentSaleId,
      long version
  ) {
    if (Objects.requireNonNull(status).isLinkedToSale() != (currentSaleId != null)) {
      throw new IllegalArgumentException("Venda atual " + currentSaleId + " incompatível com o status " + status);
    }
    return new Vehicle(id, details, status, currentSaleId, version);
  }

  /**
   * Substitui os dados do veículo. Só é permitido em Recebido, Em Preparação e Disponível.
   *
   * @throws BusinessRuleException se o status atual não permite edição
   */
  public void update(VehicleDetails newDetails) {
    if (!status.isEditable()) {
      throw new BusinessRuleException("O veículo não pode ser editado com status " + status);
    }
    details = Objects.requireNonNull(newDetails);
  }

  /**
   * Recebido → Em Preparação.
   */
  public void startPreparation() {
    transition(VehicleStatus.RECEIVED, VehicleStatus.IN_PREPARATION, "Início da preparação", null);
  }

  /**
   * Em Preparação → Disponível.
   */
  public void releaseForSale() {
    transition(VehicleStatus.IN_PREPARATION, VehicleStatus.AVAILABLE, "Liberado para venda", null);
  }

  /**
   * Disponível → Reservado, quando uma venda é criada. A venda passa a ser a venda atual.
   */
  public void reserve(UUID saleId) {
    transition(VehicleStatus.AVAILABLE, VehicleStatus.RESERVED, "Venda criada", Objects.requireNonNull(saleId));
    currentSaleId = saleId;
  }

  /**
   * Reservado → Vendido, quando o pagamento da venda atual é confirmado.
   *
   * @throws BusinessRuleException se {@code saleId} não for a venda atual
   */
  public void confirmSale(UUID saleId) {
    requireCurrentSale(saleId);
    transition(VehicleStatus.RESERVED, VehicleStatus.SOLD, "Pagamento confirmado", saleId);
  }

  /**
   * Reservado → Disponível, quando o pagamento da venda atual é recusado ou cancelado antes de ser pago. O veículo
   * fica sem venda atual.
   *
   * @throws BusinessRuleException se {@code saleId} não for a venda atual
   */
  public void releaseReservation(UUID saleId) {
    requireCurrentSale(saleId);
    transition(VehicleStatus.RESERVED, VehicleStatus.AVAILABLE, "Pagamento recusado ou cancelado", saleId);
    currentSaleId = null;
  }

  /**
   * Vendido → Disponível, quando o pagamento da venda atual é cancelado depois de pago (estorno). O veículo fica sem
   * venda atual.
   *
   * @throws BusinessRuleException se {@code saleId} não for a venda atual
   */
  public void reverseSale(UUID saleId) {
    requireCurrentSale(saleId);
    transition(VehicleStatus.SOLD, VehicleStatus.AVAILABLE, "Pagamento estornado", saleId);
    currentSaleId = null;
  }

  /**
   * Vendido → Recebido: o veículo volta à loja com um novo valor de entrada e fica sem venda atual. O valor anterior
   * fica registrado no motivo do histórico.
   */
  public void readmit(Money newPurchasePrice) {
    requireStatus(VehicleStatus.SOLD, VehicleStatus.RECEIVED);
    var previousPurchasePrice = details.purchasePrice();
    details = details.withPurchasePrice(newPurchasePrice);
    changeStatus(VehicleStatus.RECEIVED,
        "Reentrada na loja; valor de entrada anterior: " + previousPurchasePrice.amount().toPlainString(),
        null);
    currentSaleId = null;
  }

  /**
   * Indica se {@code saleId} é a venda atual. O pagamento de uma venda que não é a atual não muda o veículo.
   */
  public boolean isCurrentSale(UUID saleId) {
    return currentSaleId != null && currentSaleId.equals(saleId);
  }

  /**
   * Mudanças de status feitas desde que o veículo foi cadastrado ou restaurado, ainda não persistidas.
   */
  public List<VehicleStatusHistory> getStatusChanges() {
    return List.copyOf(statusChanges);
  }

  public UUID getId() {
    return id;
  }

  /**
   * Versão do veículo no banco, ou {@code null} se ele ainda não foi persistido.
   */
  public Long getVersion() {
    return version;
  }

  public VehicleDetails getDetails() {
    return details;
  }

  public String getBrand() {
    return details.brand();
  }

  public String getModel() {
    return details.model();
  }

  public int getYear() {
    return details.year();
  }

  public String getColor() {
    return details.color();
  }

  public Money getPurchasePrice() {
    return details.purchasePrice();
  }

  public Money getSalePrice() {
    return details.salePrice();
  }

  public VehicleOrigin getOrigin() {
    return details.origin();
  }

  public VehicleStatus getStatus() {
    return status;
  }

  /**
   * Venda que controla o veículo em Reservado ou Vendido, ou {@code null} nos demais status.
   */
  public UUID getCurrentSaleId() {
    return currentSaleId;
  }

  private void transition(
      VehicleStatus expected,
      VehicleStatus next,
      String reason,
      UUID saleId
  ) {
    requireStatus(expected, next);
    changeStatus(next, reason, saleId);
  }

  private void requireCurrentSale(UUID saleId) {
    if (status.isLinkedToSale() && !isCurrentSale(Objects.requireNonNull(saleId))) {
      throw new BusinessRuleException("A venda " + saleId + " não é a venda atual do veículo");
    }
  }

  private void requireStatus(
      VehicleStatus expected,
      VehicleStatus next
  ) {
    if (status != expected) {
      throw new InvalidStatusTransitionException("Não é possível mudar o status do veículo de " + status + " para " + next);
    }
  }

  private void changeStatus(
      VehicleStatus next,
      String reason,
      UUID saleId
  ) {
    statusChanges.add(VehicleStatusHistory.recordsVehicleStatus(id, status, next, reason, saleId));
    status = next;
  }
}

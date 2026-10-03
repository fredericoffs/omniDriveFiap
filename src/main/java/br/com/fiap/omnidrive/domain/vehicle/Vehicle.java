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

  // TODO: guardar a venda atual (currentSaleId), conferi-la em confirmSale, releaseReservation e reverseSale e
  //  zerá-la em releaseReservation, reverseSale e readmit (semana 3, junto com a Sale).

  private final UUID id;
  private final Long version;
  private final List<VehicleStatusHistory> statusChanges = new ArrayList<>();
  private VehicleDetails details;
  private VehicleStatus status;

  private Vehicle(
      UUID id,
      VehicleDetails details,
      VehicleStatus status,
      Long version
  ) {
    this.id = Objects.requireNonNull(id);
    this.details = Objects.requireNonNull(details);
    this.status = status;
    this.version = version;
  }

  /**
   * Cadastra um veículo que chegou à loja. Ele entra como Recebido.
   */
  public static Vehicle register(VehicleDetails details) {
    var vehicle = new Vehicle(UUID.randomUUID(), details, null, null);
    vehicle.changeStatus(VehicleStatus.RECEIVED, "Cadastro do veículo", null);
    return vehicle;
  }

  /**
   * Reconstrói um veículo já existente, sem gerar histórico. Uso exclusivo da persistência.
   *
   * @param version versão lida do banco, usada no controle de concorrência otimista
   */
  public static Vehicle restore(
      UUID id,
      VehicleDetails details,
      VehicleStatus status,
      long version
  ) {
    return new Vehicle(id, details, Objects.requireNonNull(status), version);
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
   * Disponível → Reservado, quando uma venda é criada.
   */
  public void reserve(UUID saleId) {
    transition(VehicleStatus.AVAILABLE, VehicleStatus.RESERVED, "Venda criada", Objects.requireNonNull(saleId));
  }

  /**
   * Reservado → Vendido, quando o pagamento é confirmado.
   */
  public void confirmSale(UUID saleId) {
    transition(VehicleStatus.RESERVED, VehicleStatus.SOLD, "Pagamento confirmado",
        Objects.requireNonNull(saleId));
  }

  /**
   * Reservado → Disponível, quando o pagamento é recusado ou cancelado antes de ser pago.
   */
  public void releaseReservation(UUID saleId) {
    transition(VehicleStatus.RESERVED, VehicleStatus.AVAILABLE, "Pagamento recusado ou cancelado",
        Objects.requireNonNull(saleId));
  }

  /**
   * Vendido → Disponível, quando o pagamento é cancelado depois de pago (estorno).
   */
  public void reverseSale(UUID saleId) {
    transition(VehicleStatus.SOLD, VehicleStatus.AVAILABLE, "Pagamento estornado",
        Objects.requireNonNull(saleId));
  }

  /**
   * Vendido → Recebido: o veículo volta à loja com um novo valor de entrada. O valor anterior fica registrado no motivo do histórico.
   */
  public void readmit(Money newPurchasePrice) {
    requireStatus(VehicleStatus.SOLD, VehicleStatus.RECEIVED);
    var previousPurchasePrice = details.purchasePrice();
    details = details.withPurchasePrice(newPurchasePrice);
    changeStatus(VehicleStatus.RECEIVED,
        "Reentrada na loja; valor de entrada anterior: " + previousPurchasePrice.amount().toPlainString(),
        null);
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

  private void transition(
      VehicleStatus expected,
      VehicleStatus next,
      String reason,
      UUID saleId
  ) {
    requireStatus(expected, next);
    changeStatus(next, reason, saleId);
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

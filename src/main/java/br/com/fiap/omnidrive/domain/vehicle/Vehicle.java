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
 * <p>O cadastro é único: um veículo vendido pode voltar à loja pela reentrada e ser vendido de novo. O chassi
 * ({@link Chassis}) identifica o veículo, não muda depois do cadastro e precisa ser compatível com o ano informado no
 * cadastro e na edição. Toda mudança
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
 *     <li>Disponível → Em Preparação, para reparo ({@link #startPreparation()})</li>
 *     <li>Disponível → Reservado ({@link #reserve(UUID)})</li>
 *     <li>Reservado → Vendido ({@link #confirmSale(UUID)})</li>
 *     <li>Reservado → Disponível ({@link #releaseReservation(UUID)})</li>
 *     <li>Vendido → Disponível ({@link #reverseSale(UUID)})</li>
 *     <li>Vendido → Recebido ({@link #readmit(Money, Integer)})</li>
 * </ul>
 */
public class Vehicle {

  private final UUID id;
  private final Chassis chassis;
  private final Long version;
  private final List<VehicleStatusHistory> statusChanges = new ArrayList<>();
  private VehicleDetails details;
  private VehicleStatus status;
  private UUID currentSaleId;

  private Vehicle(
      UUID id,
      Chassis chassis,
      VehicleDetails details,
      VehicleStatus status,
      UUID currentSaleId,
      Long version
  ) {
    this.id = Objects.requireNonNull(id);
    this.chassis = Objects.requireNonNull(chassis);
    this.details = Objects.requireNonNull(details);
    this.status = status;
    this.currentSaleId = currentSaleId;
    this.version = version;
  }

  /**
   * Cadastra um veículo que chegou à loja. Ele entra como Recebido.
   *
   * @throws BusinessRuleException se o chassi não for informado ou não for compatível com o ano
   */
  public static Vehicle register(
      Chassis chassis,
      VehicleDetails details
  ) {
    if (chassis == null) {
      throw new BusinessRuleException("O chassi é obrigatório");
    }
    chassis.requireCompatibleWith(details.year());
    var vehicle = new Vehicle(
        UUID.randomUUID(),
        chassis,
        details,
        null,
        null,
        null
    );
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
      Chassis chassis,
      VehicleDetails details,
      VehicleStatus status,
      UUID currentSaleId,
      long version
  ) {
    if (Objects.requireNonNull(status).isLinkedToSale() == (currentSaleId == null)) {
      throw new IllegalArgumentException("Venda atual " + currentSaleId + " incompatível com o status " + status);
    }
    return new Vehicle(id, chassis, details, status, currentSaleId, version);
  }

  /**
   * Substitui os dados do veículo. Só é permitido em Recebido, Em Preparação e Disponível, e o novo ano precisa ser
   * compatível com o chassi.
   *
   * @throws BusinessRuleException se o status atual não permite edição ou o ano não for compatível com o chassi
   */
  public void update(VehicleDetails newDetails) {
    if (!status.isEditable()) {
      throw new BusinessRuleException("O veículo não pode ser editado com status " + status);
    }
    chassis.requireCompatibleWith(newDetails.year());
    details = newDetails;
  }

  /**
   * Recebido → Em Preparação, na entrada do veículo, ou Disponível → Em Preparação, quando ele sai da vitrine para
   * reparo. É a única volta permitida na entrada: Recebido → Em Preparação → Disponível continua sempre nessa ordem.
   * O motivo do histórico indica qual das duas aconteceu.
   *
   * @throws InvalidStatusTransitionException se o veículo não estiver Recebido nem Disponível
   */
  public void startPreparation() {
    var reason = switch (status) {
      case RECEIVED -> "Início da preparação";
      case AVAILABLE -> "Retirado da vitrine para reparo";
      default -> throw invalidTransition(VehicleStatus.IN_PREPARATION);
    };
    changeStatus(VehicleStatus.IN_PREPARATION, reason, null);
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
   * Vendido → Recebido: o veículo volta à loja com um novo valor de entrada e uma nova quilometragem, e fica sem venda
   * atual. O valor de entrada e a quilometragem anteriores ficam registrados no motivo do histórico.
   *
   * @throws BusinessRuleException se a quilometragem não for informada ou não for maior que a atual
   */
  public void readmit(
      Money newPurchasePrice,
      Integer newMileage
  ) {
    requireStatus(VehicleStatus.SOLD, VehicleStatus.RECEIVED);
    if (newMileage == null) {
      throw new BusinessRuleException("A quilometragem é obrigatória");
    }
    var previous = details;
    if (newMileage <= previous.mileage()) {
      throw new BusinessRuleException(
          "A quilometragem da reentrada deve ser maior que a atual (" + previous.mileage() + " km)");
    }
    details = previous.withReadmission(newPurchasePrice, newMileage);
    changeStatus(VehicleStatus.RECEIVED,
        "Reentrada na loja; valor de entrada anterior: " + previous.purchasePrice().amount().toPlainString()
            + "; quilometragem anterior: " + previous.mileage() + " km",
        null);
    currentSaleId = null;
  }

  /**
   * Confere se o veículo pode ser excluído. Só o veículo Recebido que nunca teve venda pode ser excluído, para corrigir
   * um cadastro errado. Um veículo que voltou pela reentrada também está Recebido, mas já teve venda.
   *
   * @param hasSales se o veículo tem alguma venda registrada, atual ou antiga
   * @throws BusinessRuleException se o veículo não estiver Recebido ou já tiver tido venda
   */
  public void ensureCanBeDeleted(boolean hasSales) {
    if (status != VehicleStatus.RECEIVED) {
      throw new BusinessRuleException("O veículo não pode ser excluído com status " + status);
    }
    if (hasSales) {
      throw new BusinessRuleException("O veículo já teve venda e não pode ser excluído");
    }
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

  public Chassis getChassis() {
    return chassis;
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

  public Plate getPlate() {
    return details.plate();
  }

  public int getMileage() {
    return details.mileage();
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
      throw invalidTransition(next);
    }
  }

  private InvalidStatusTransitionException invalidTransition(VehicleStatus next) {
    return new InvalidStatusTransitionException("Não é possível mudar o status do veículo de " + status + " para " + next);
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

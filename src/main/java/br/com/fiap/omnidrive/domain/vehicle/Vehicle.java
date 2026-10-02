package br.com.fiap.omnidrive.domain.vehicle;

import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;

import java.time.Year;
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

  private static final int MIN_YEAR = 1900;

  private final UUID id;
  private final Long version;
  private final List<VehicleStatusHistory> statusChanges = new ArrayList<>();
  private String brand;
  private String model;
  private int year;
  private String color;
  private Money purchasePrice;
  private Money salePrice;
  private VehicleOrigin origin;
  private VehicleStatus status;

  private Vehicle(
      UUID id,
      VehicleStatus status,
      Long version
  ) {
    this.id = Objects.requireNonNull(id);
    this.status = status;
    this.version = version;
  }

  /**
   * Cadastra um veículo que chegou à loja. Ele entra como Recebido.
   *
   * @param purchasePrice valor de entrada: quanto a loja pagou ou acordou com o dono, se consignado
   * @param salePrice     preço de venda anunciado
   */
  public static Vehicle register(
      String brand,
      String model,
      int year,
      String color,
      Money purchasePrice,
      Money salePrice,
      VehicleOrigin origin
  ) {
    var vehicle = new Vehicle(UUID.randomUUID(), null, null);
    vehicle.fill(brand, model, year, color, purchasePrice, salePrice, origin);
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
      String brand,
      String model,
      int year,
      String color,
      Money purchasePrice,
      Money salePrice,
      VehicleOrigin origin,
      VehicleStatus status,
      long version
  ) {
    var vehicle = new Vehicle(id, Objects.requireNonNull(status), version);
    vehicle.fill(brand, model, year, color, purchasePrice, salePrice, origin);
    return vehicle;
  }

  private static int validateYear(int year) {
    var maxYear = Year.now().getValue() + 1;
    if (year < MIN_YEAR || year > maxYear) {
      throw new BusinessRuleException("O ano deve estar entre " + MIN_YEAR + " e " + maxYear);
    }
    return year;
  }

  private static String requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new BusinessRuleException(message);
    }
    return value.strip();
  }

  private static <T> T require(T value, String message) {
    if (value == null) {
      throw new BusinessRuleException(message);
    }
    return value;
  }

  /**
   * Substitui os dados do veículo. Só é permitido em Recebido, Em Preparação e Disponível.
   *
   * @throws BusinessRuleException se o status atual não permite edição ou algum dado for inválido
   */
  public void update(
      String brand,
      String model,
      int year,
      String color,
      Money purchasePrice,
      Money salePrice,
      VehicleOrigin origin
  ) {
    if (!status.isEditable()) {
      throw new BusinessRuleException("O veículo não pode ser editado com status " + status);
    }
    fill(brand, model, year, color, purchasePrice, salePrice, origin);
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
    var previousPurchasePrice = purchasePrice;
    purchasePrice = require(newPurchasePrice, "O valor de entrada é obrigatório");
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

  public String getBrand() {
    return brand;
  }

  public String getModel() {
    return model;
  }

  public int getYear() {
    return year;
  }

  public String getColor() {
    return color;
  }

  public Money getPurchasePrice() {
    return purchasePrice;
  }

  public Money getSalePrice() {
    return salePrice;
  }

  public VehicleOrigin getOrigin() {
    return origin;
  }

  public VehicleStatus getStatus() {
    return status;
  }

  private void fill(
      String brand,
      String model,
      int year,
      String color,
      Money purchasePrice,
      Money salePrice,
      VehicleOrigin origin
  ) {
    this.brand = requireText(brand, "A marca é obrigatória");
    this.model = requireText(model, "O modelo é obrigatório");
    this.year = validateYear(year);
    this.color = requireText(color, "A cor é obrigatória");
    this.purchasePrice = require(purchasePrice, "O valor de entrada é obrigatório");
    this.salePrice = require(salePrice, "O preço de venda é obrigatório");
    this.origin = require(origin, "A origem é obrigatória");
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
    statusChanges.add(VehicleStatusHistory.record(id, status, next, reason, saleId));
    status = next;
  }
}

package br.com.fiap.omnidrive.domain.sale;

import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;

import java.util.Objects;
import java.util.UUID;

/**
 * Pagamento de uma venda, identificado pelo código que o processador de pagamento usa no webhook. O código é gerado
 * pela API quando a venda é criada.
 *
 * <p>A versão ({@link #getVersion()}) serve ao controle de concorrência otimista, como no veículo.
 *
 * <p>Efeito de cada resultado ({@link #register(PaymentResult)}):
 * <ul>
 *     <li>Aguardando Pagamento → Pago, Recusado ou Cancelado</li>
 *     <li>Pago + {@code CANCELED} → Estornado</li>
 *     <li>Repetição de um resultado já aplicado (inclusive Estornado + {@code CANCELED}): nenhuma mudança</li>
 *     <li>Qualquer outra combinação: {@link InvalidStatusTransitionException}. Os estados terminais não são
 *     reconciliados.</li>
 * </ul>
 */
public class Payment {

  private final UUID id;
  private final String paymentCode;
  private final UUID saleId;
  private final Long version;
  private PaymentStatus status;

  private Payment(
      UUID id,
      String paymentCode,
      UUID saleId,
      PaymentStatus status,
      Long version
  ) {
    this.id = Objects.requireNonNull(id);
    this.paymentCode = Objects.requireNonNull(paymentCode);
    this.saleId = Objects.requireNonNull(saleId);
    this.status = Objects.requireNonNull(status);
    this.version = version;
  }

  /**
   * Abre o pagamento de uma venda recém-criada, em Aguardando Pagamento e com um código novo.
   */
  public static Payment open(UUID saleId) {
    return new Payment(
        UUID.randomUUID(),
        UUID.randomUUID().toString(),
        saleId,
        PaymentStatus.AWAITING_PAYMENT,
        null
    );
  }

  /**
   * Reconstrói um pagamento já existente. Uso exclusivo da persistência.
   *
   * @param version versão lida do banco, usada no controle de concorrência otimista
   */
  public static Payment restore(
      UUID id,
      String paymentCode,
      UUID saleId,
      PaymentStatus status,
      long version
  ) {
    return new Payment(id, paymentCode, saleId, status, version);
  }

  /**
   * Aplica o resultado informado pelo processador.
   *
   * @return {@code true} se o status mudou, {@code false} se o resultado é uma repetição
   * @throws InvalidStatusTransitionException se o resultado contradiz o status atual
   */
  public boolean register(PaymentResult result) {
    var next = nextStatus(Objects.requireNonNull(result));
    if (next == status) {
      return false;
    }
    status = next;
    return true;
  }

  public UUID getId() {
    return id;
  }

  public String getPaymentCode() {
    return paymentCode;
  }

  public UUID getSaleId() {
    return saleId;
  }

  /**
   * Versão do pagamento no banco, ou {@code null} se ele ainda não foi persistido.
   */
  public Long getVersion() {
    return version;
  }

  public PaymentStatus getStatus() {
    return status;
  }

  private PaymentStatus nextStatus(PaymentResult result) {
    return switch (status) {
      case AWAITING_PAYMENT -> switch (result) {
        case PAID -> PaymentStatus.PAID;
        case DECLINED -> PaymentStatus.DECLINED;
        case CANCELED -> PaymentStatus.CANCELED;
      };
      case PAID -> switch (result) {
        case PAID -> PaymentStatus.PAID;
        case CANCELED -> PaymentStatus.REFUNDED;
        case DECLINED -> throw invalid(result);
      };
      case DECLINED -> requireRepetition(result, PaymentResult.DECLINED);
      case CANCELED, REFUNDED -> requireRepetition(result, PaymentResult.CANCELED);
    };
  }

  private PaymentStatus requireRepetition(
      PaymentResult result,
      PaymentResult repeated
  ) {
    if (result != repeated) {
      throw invalid(result);
    }
    return status;
  }

  private InvalidStatusTransitionException invalid(PaymentResult result) {
    return new InvalidStatusTransitionException(
        "Não é possível registrar o resultado " + result + " em um pagamento com status " + status);
  }
}

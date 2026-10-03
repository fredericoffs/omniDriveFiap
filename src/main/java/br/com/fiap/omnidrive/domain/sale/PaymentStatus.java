package br.com.fiap.omnidrive.domain.sale;

/**
 * Status do pagamento de uma venda. O processador de pagamento é a fonte da verdade, e as transições permitidas ficam
 * em {@link Payment}.
 */
public enum PaymentStatus {

  /**
   * A venda foi criada e o processador ainda não informou o resultado.
   */
  AWAITING_PAYMENT,

  /**
   * O processador confirmou o pagamento.
   */
  PAID,

  /**
   * O processador recusou o pagamento. Estado terminal: uma nova tentativa é uma nova venda.
   */
  DECLINED,

  /**
   * O pagamento foi cancelado antes de ser pago. Estado terminal.
   */
  CANCELED,

  /**
   * O pagamento foi cancelado depois de pago. Estado terminal.
   */
  REFUNDED
}

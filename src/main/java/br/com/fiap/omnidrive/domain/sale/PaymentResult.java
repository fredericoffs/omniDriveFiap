package br.com.fiap.omnidrive.domain.sale;

/**
 * Resultado que o processador de pagamento informa pelo webhook. O efeito de cada resultado depende do status atual do
 * {@link Payment}.
 */
public enum PaymentResult {

  /**
   * O pagamento foi efetuado.
   */
  PAID,

  /**
   * O pagamento foi recusado.
   */
  DECLINED,

  /**
   * O pagamento foi cancelado. Se já estava pago, vira estorno.
   */
  CANCELED
}

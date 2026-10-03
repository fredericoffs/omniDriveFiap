package br.com.fiap.omnidrive.domain.vehicle;

/**
 * Status do veículo na loja: Recebido → Em Preparação → Disponível → Reservado → Vendido. As transições permitidas ficam em {@link Vehicle}.
 */
public enum VehicleStatus {

  /**
   * O veículo chegou à loja (cadastro ou reentrada).
   */
  RECEIVED(true),

  /**
   * Em revisão, higienização ou funilaria. Todos os veículos passam por aqui.
   */
  IN_PREPARATION(true),

  /**
   * Pronto para venda. É o único status que aparece na listagem "à venda".
   */
  AVAILABLE(true),

  /**
   * Há uma venda aguardando o resultado do pagamento.
   */
  RESERVED(false),

  /**
   * O pagamento da venda foi confirmado.
   */
  SOLD(false);

  private final boolean editable;

  VehicleStatus(boolean editable) {
    this.editable = editable;
  }

  /**
   * Indica se os dados do veículo podem ser editados neste status.
   */
  public boolean isEditable() {
    return editable;
  }
}

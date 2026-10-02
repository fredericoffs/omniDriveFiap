package br.com.fiap.omnidrive.domain.vehicle;

/**
 * Origem do veículo. É um atributo e não interfere no ciclo de status.
 */
public enum VehicleOrigin {

  /**
   * Veículo da própria loja.
   */
  OWNED,

  /**
   * Veículo de terceiro, exposto para venda mediante comissão.
   */
  CONSIGNED
}

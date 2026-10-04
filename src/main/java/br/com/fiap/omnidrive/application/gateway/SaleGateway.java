package br.com.fiap.omnidrive.application.gateway;

import br.com.fiap.omnidrive.domain.sale.Sale;

/**
 * Persistência de vendas, implementada pela camada de infraestrutura.
 */
public interface SaleGateway {

  /**
   * Grava uma venda nova. A venda não muda depois de criada.
   *
   * @return a venda como ficou gravada
   */
  Sale save(Sale sale);
}

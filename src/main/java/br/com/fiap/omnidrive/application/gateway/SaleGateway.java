package br.com.fiap.omnidrive.application.gateway;

import br.com.fiap.omnidrive.domain.sale.Sale;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

  /**
   * Busca uma venda pelo id.
   */
  Optional<Sale> findById(UUID id);

  /**
   * Busca as vendas com os ids informados, sem ordem definida. Ids sem venda são ignorados.
   */
  List<Sale> findAllById(Collection<UUID> ids);
}

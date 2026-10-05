package br.com.fiap.omnidrive.application.pagination;

/**
 * Página pedida numa listagem.
 *
 * <p>A camada web valida os parâmetros antes de criar a página e responde 400 se estiverem fora dos limites. As
 * validações daqui protegem o contrato dos casos de uso e dos gateways.
 *
 * @param page número da página, a partir de zero
 * @param size quantidade de itens por página, de 1 a {@value #MAX_SIZE}
 */
public record PageQuery(
    int page,
    int size
) {

  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  public PageQuery {
    if (page < 0) {
      throw new IllegalArgumentException("A página deve ser maior ou igual a zero");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new IllegalArgumentException("O tamanho da página deve estar entre 1 e " + MAX_SIZE);
    }
  }

  /**
   * Primeira página, com o tamanho padrão de {@value #DEFAULT_SIZE} itens.
   */
  public static PageQuery first() {
    return new PageQuery(0, DEFAULT_SIZE);
  }
}

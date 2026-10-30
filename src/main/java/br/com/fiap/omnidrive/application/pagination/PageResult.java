package br.com.fiap.omnidrive.application.pagination;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Página de uma listagem, com os itens dela e o total de itens de todas as páginas.
 *
 * @param content       itens da página, na ordem da listagem
 * @param page          número da página, a partir de zero
 * @param size          quantidade máxima de itens por página
 * @param totalElements total de itens da listagem, somando todas as páginas
 * @param <T>           tipo dos itens
 */
public record PageResult<T>(
    List<T> content,
    int page,
    int size,
    long totalElements
) {

  public PageResult {
    content = List.copyOf(Objects.requireNonNull(content));
    if (page < 0) {
      throw new IllegalArgumentException("A página deve ser maior ou igual a zero");
    }
    if (size < 1) {
      throw new IllegalArgumentException("O tamanho da página deve ser maior que zero");
    }
    if (content.size() > size) {
      throw new IllegalArgumentException("A página tem mais itens que o tamanho dela");
    }
    if (totalElements < content.size()) {
      throw new IllegalArgumentException("O total de itens é menor que a quantidade de itens da página");
    }
  }

  /**
   * Quantidade de páginas da listagem. É zero quando a listagem está vazia.
   */
  public int totalPages() {
    return Math.toIntExact(Math.ceilDiv(totalElements, size));
  }

  /**
   * Converte os itens da página, mantendo a ordem e os dados de paginação.
   */
  public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
    return new PageResult<>(
        content.stream().<R>map(mapper).toList(),
        page,
        size,
        totalElements
    );
  }
}

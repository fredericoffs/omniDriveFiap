package br.com.fiap.omnidrive.infrastructure.persistence;

import br.com.fiap.omnidrive.application.pagination.PageQuery;
import br.com.fiap.omnidrive.application.pagination.PageResult;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Conversão entre os tipos de paginação da aplicação ({@link PageQuery} e {@link PageResult}) e os do Spring Data.
 * A ordenação não vai no {@link Pageable}: ela é fixa e fica em cada consulta do repositório.
 */
final class PageConversion {

  private PageConversion() {
  }

  static Pageable toPageable(PageQuery pageQuery) {
    return PageRequest.of(pageQuery.page(), pageQuery.size());
  }

  static <E, T> PageResult<T> toResult(
      Page<E> page,
      Function<? super E, ? extends T> mapper
  ) {
    return new PageResult<>(
        page.getContent().stream().<T>map(mapper).toList(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements()
    );
  }
}

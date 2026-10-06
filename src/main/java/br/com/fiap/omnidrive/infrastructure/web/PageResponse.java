package br.com.fiap.omnidrive.infrastructure.web;

import br.com.fiap.omnidrive.application.pagination.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;

/**
 * Representação de uma página de listagem, com os itens e os dados para navegar entre as páginas.
 *
 * @param <T> tipo dos itens
 */
@Schema(description = "Página de uma listagem")
public record PageResponse<T>(

    @Schema(description = "Itens da página, na ordem da listagem", requiredMode = Schema.RequiredMode.REQUIRED)
    List<T> content,

    @Schema(
        description = "Número da página, a partir de zero",
        example = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    int page,

    @Schema(
        description = "Quantidade máxima de itens por página",
        example = "20",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    int size,

    @Schema(
        description = "Total de itens da listagem, somando todas as páginas",
        example = "42",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    long totalElements,

    @Schema(
        description = "Quantidade de páginas; zero se a listagem estiver vazia",
        example = "3",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    int totalPages
) {

  /**
   * Cria a representação da página, convertendo cada item com {@code mapper}.
   */
  public static <T, R> PageResponse<R> from(
      PageResult<T> page,
      Function<? super T, ? extends R> mapper
  ) {
    var converted = page.<R>map(mapper);
    return new PageResponse<>(
        converted.content(),
        converted.page(),
        converted.size(),
        converted.totalElements(),
        converted.totalPages()
    );
  }
}

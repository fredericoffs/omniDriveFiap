package br.com.fiap.omnidrive.infrastructure.web;

import br.com.fiap.omnidrive.application.pagination.PageQuery;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Parâmetros de paginação das listagens ({@code ?page=0&size=20}). Os dois são opcionais: sem eles, vale a primeira
 * página com {@value PageQuery#DEFAULT_SIZE} itens. Valores fora dos limites respondem 400.
 *
 * @param page número da página, a partir de zero
 * @param size quantidade de itens por página, de 1 a {@value PageQuery#MAX_SIZE}
 */
public record PageParams(

    @Parameter(
        description = "Número da página, a partir de zero",
        schema = @Schema(type = "integer", defaultValue = "0", minimum = "0")
    )
    @Min(value = 0, message = "A página deve ser maior ou igual a zero")
    Integer page,

    @Parameter(
        description = "Quantidade de itens por página, de 1 a " + PageQuery.MAX_SIZE,
        schema = @Schema(
            type = "integer",
            defaultValue = "" + PageQuery.DEFAULT_SIZE,
            minimum = "1",
            maximum = "" + PageQuery.MAX_SIZE
        )
    )
    @Min(value = 1, message = "O tamanho da página deve estar entre 1 e " + PageQuery.MAX_SIZE)
    @Max(value = PageQuery.MAX_SIZE, message = "O tamanho da página deve estar entre 1 e " + PageQuery.MAX_SIZE)
    Integer size
) {

  /**
   * Converte os parâmetros na página pedida, aplicando os valores padrão aos que não foram informados.
   */
  public PageQuery toPageQuery() {
    return new PageQuery(
        page == null ? 0 : page,
        size == null ? PageQuery.DEFAULT_SIZE : size
    );
  }
}

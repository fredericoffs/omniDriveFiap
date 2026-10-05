package br.com.fiap.omnidrive.application.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes da página pedida {@link PageQuery}.
 */
class PageQueryTest {

  @ParameterizedTest
  @CsvSource({
      "0, 1",
      "0, 100",
      "3, 20"
  })
  void acceptsPageAndSizeWithinLimits(
      int page,
      int size
  ) {
    assertThat(new PageQuery(page, size))
        .returns(page, PageQuery::page)
        .returns(size, PageQuery::size);
  }

  @Test
  void rejectsNegativePage() {
    assertThatThrownBy(() -> new PageQuery(-1, 20))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("A página deve ser maior ou igual a zero");
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 101})
  void rejectsSizeOutsideLimits(int size) {
    assertThatThrownBy(() -> new PageQuery(0, size))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("O tamanho da página deve estar entre 1 e 100");
  }

  @Test
  void firstIsPageZeroWithDefaultSize() {
    assertThat(PageQuery.first()).isEqualTo(new PageQuery(0, 20));
  }
}

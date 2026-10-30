package br.com.fiap.omnidrive.application.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Testes da página de listagem {@link PageResult}.
 */
class PageResultTest {

  @ParameterizedTest
  @CsvSource({
      "0, 20, 0",
      "1, 20, 1",
      "20, 20, 1",
      "21, 20, 2",
      "100, 10, 10"
  })
  void calculatesTotalPages(
      long totalElements,
      int size,
      int expected
  ) {
    var result = new PageResult<>(List.of(), 5, size, totalElements);

    assertThat(result.totalPages()).isEqualTo(expected);
  }

  @Test
  void mapConvertsContentKeepingOrderAndPagination() {
    var result = new PageResult<>(List.of(3, 1, 2), 1, 3, 7);

    assertThat(result.map(String::valueOf))
        .isEqualTo(new PageResult<>(List.of("3", "1", "2"), 1, 3, 7));
  }

  @Test
  void keepsAnUnmodifiableCopyOfContent() {
    var content = new ArrayList<>(List.of("a"));
    var result = new PageResult<>(content, 0, 20, 1);
    content.add("b");

    assertThat(result.content())
        .containsExactly("a")
        .isUnmodifiable();
  }

  @Test
  void rejectsNegativePage() {
    var content = List.of();

    assertThatThrownBy(() -> new PageResult<>(content, -1, 20, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("A página deve ser maior ou igual a zero");
  }

  @Test
  void rejectsSizeLessThanOne() {
    var content = List.of();

    assertThatThrownBy(() -> new PageResult<>(content, 0, 0, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("O tamanho da página deve ser maior que zero");
  }

  @Test
  void rejectsMoreItemsThanSize() {
    var content = List.of("a", "b");

    assertThatThrownBy(() -> new PageResult<>(content, 0, 1, 2))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("A página tem mais itens que o tamanho dela");
  }

  @Test
  void rejectsTotalLessThanContentSize() {
    var content = List.of("a", "b");

    assertThatThrownBy(() -> new PageResult<>(content, 0, 20, 1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("O total de itens é menor que a quantidade de itens da página");
  }
}

package br.com.fiap.omnidrive.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do objeto de valor {@link Money}.
 */
class MoneyTest {

  @Test
  void normalizesToTwoDecimalPlaces() {
    assertThat(Money.of("1500").amount()).isEqualTo(new BigDecimal("1500.00"));
    assertThat(Money.of("1500.5")).isEqualTo(Money.of("1500.50"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "0.00"})
  void rejectsAmountLessThanOrEqualToZero(String amount) {
    assertThatThrownBy(() -> Money.of(amount))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O valor deve ser maior que zero");
  }

  @Test
  void rejectsMoreThanTwoDecimalPlaces() {
    assertThatThrownBy(() -> Money.of("10.001"))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O valor deve ter no máximo duas casas decimais");
  }

  @Test
  void acceptsTrailingZerosBeyondSecondDecimalPlace() {
    assertThat(Money.of("10.5000").amount()).isEqualTo(new BigDecimal("10.50"));
  }

  @Test
  void rejectsNullAmount() {
    assertThatThrownBy(() -> new Money(null))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O valor é obrigatório");
  }

  @Test
  void ofNullableKeepsNull() {
    assertThat(Money.ofNullable(null)).isNull();
    assertThat(Money.ofNullable(new BigDecimal("10"))).isEqualTo(Money.of("10.00"));
  }
}

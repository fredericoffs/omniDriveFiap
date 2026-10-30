package br.com.fiap.omnidrive.domain.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do objeto de valor {@link Cpf}.
 */
class CpfTest {

  @ParameterizedTest
  @ValueSource(strings = {"52998224725", "529.982.247-25", " 529.982.247-25 ", "529982247-25"})
  void keepsOnlyDigits(String number) {
    assertThat(new Cpf(number).number()).isEqualTo("52998224725");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void rejectsMissingNumber(String number) {
    assertThatThrownBy(() -> new Cpf(number))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O CPF é obrigatório");
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "52998224726",
      "52998224715",
      "5299822472",
      "529982247250",
      "529-982-247.25",
      "52998a24725",
      "11111111111",
      "000.000.000-00"
  })
  void rejectsInvalidNumber(String number) {
    assertThatThrownBy(() -> new Cpf(number))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O CPF é inválido");
  }
}

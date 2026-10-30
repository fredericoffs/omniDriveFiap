package br.com.fiap.omnidrive.domain.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do objeto de valor {@link Plate}.
 */
class PlateTest {

  @ParameterizedTest
  @CsvSource({
      "ABC1234,   ABC1234",
      "ABC-1234,  ABC1234",
      "abc-1234,  ABC1234",
      "ABC1D23,   ABC1D23",
      "abc1d23,   ABC1D23",
      "' BRA2E19 ', BRA2E19"
  })
  void acceptsOldAndMercosulFormats(
      String input,
      String expected
  ) {
    assertThat(new Plate(input).value()).isEqualTo(expected);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void rejectsMissingPlate(String input) {
    assertThatThrownBy(() -> new Plate(input))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("A placa é obrigatória");
  }

  @ParameterizedTest
  @ValueSource(strings = {"AB1234", "ABCD123", "ABC12345", "1BC1234", "ABC1DD3", "ABC-1D23", "ABC 1234", "ÁBC1234"})
  void rejectsPlateOutOfFormat(String input) {
    assertThatThrownBy(() -> new Plate(input))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("A placa deve seguir o padrão antigo (ABC-1234) ou o Mercosul (ABC1D23)");
  }
}

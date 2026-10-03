package br.com.fiap.omnidrive.domain.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes do objeto de valor {@link Chassis}: formato, dígito verificador do VIN e compatibilidade com o ano.
 */
class ChassisTest {

  @Test
  void stripsAndUppercases() {
    assertThat(new Chassis(" 9bwzzz377lt004251 ").value()).isEqualTo("9BWZZZ377LT004251");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  void rejectsMissingChassis(String value) {
    assertThatThrownBy(() -> new Chassis(value))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O chassi é obrigatório");
  }

  @Test
  void rejectsChassisLongerThanThirtyCharacters() {
    var value = "A".repeat(31);

    assertThatThrownBy(() -> new Chassis(value))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O chassi deve ter no máximo 30 caracteres");
  }

  @ParameterizedTest
  @ValueSource(strings = {"9BWZZZ377IT004251", "9BWZZZ377OT004251", "9BWZZZ377QT004251", "9BWZZZ377-T004251"})
  void rejectsVinWithInvalidCharacters(String value) {
    assertThatThrownBy(() -> new Chassis(value))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O VIN deve conter só letras e números, sem I, O e Q");
  }

  @ParameterizedTest
  @CsvSource({
      "9BWZZZ377LT004251, true",
      "BF0123456,         false",
      "ABC-123/OQ,        false"
  })
  void identifiesVin(
      String value,
      boolean expected
  ) {
    assertThat(new Chassis(value).isVin()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "1M8GDM9AXKP042788, true",
      "1HGCM82633A004352, true",
      "11111111111111111, true",
      "9BWZZZ377VT004251, false",
      "BF0123456,         false"
  })
  void checksVinCheckDigit(
      String value,
      boolean expected
  ) {
    assertThat(new Chassis(value).hasValidCheckDigit()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "9BWZZZ377LT004251, 2020",
      "1HGCM82633A004352, 2003",
      "9BWZZZ377VT004251, 1997",
      "9BWZZZ377VT004251, 1998",
      "9BWZZZ377VT004251, 2027",
      "BF0123456,         1986",
      "BF0123456,         1900"
  })
  void acceptsChassisCompatibleWithYear(
      String value,
      int year
  ) {
    var chassis = new Chassis(value);

    assertThatCode(() -> chassis.requireCompatibleWith(year)).doesNotThrowAnyException();
  }

  @ParameterizedTest
  @CsvSource({
      "9BWZZZ377LT004251, 2021, L",
      "9BWZZZ377VT004251, 1996, V",
      "1HGCM82633A004352, 2004, 3"
  })
  void rejectsVinWhoseYearCodeDoesNotMatch(
      String value,
      int year,
      char code
  ) {
    var chassis = new Chassis(value);

    assertThatThrownBy(() -> chassis.requireCompatibleWith(year))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O ano " + year + " não corresponde ao código de ano do VIN (10ª posição: " + code + ")");
  }

  @Test
  void rejectsNonVinChassisFrom1987() {
    var chassis = new Chassis("BF0123456");

    assertThatThrownBy(() -> chassis.requireCompatibleWith(1987))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("O chassi deve ser um VIN de 17 caracteres para veículos com ano a partir de 1987");
  }
}

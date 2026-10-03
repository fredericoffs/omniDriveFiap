package br.com.fiap.omnidrive.domain.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import java.time.Year;
import java.time.ZoneId;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testes das validações dos dados cadastrais do veículo ({@link VehicleDetails}).
 */
class VehicleDetailsTest {

  private static final Money PRICE = Money.of("30000");

  private static final int NEXT_YEAR = Year.now(ZoneId.of("America/Sao_Paulo")).getValue() + 1;

  static Stream<Arguments> missingRequiredFields() {
    return Stream.of(
        Arguments.of(" ", "Uno", "Prata", PRICE, PRICE, VehicleOrigin.OWNED, "A marca é obrigatória"),
        Arguments.of("Fiat", null, "Prata", PRICE, PRICE, VehicleOrigin.OWNED, "O modelo é obrigatório"),
        Arguments.of("Fiat", "Uno", "", PRICE, PRICE, VehicleOrigin.OWNED, "A cor é obrigatória"),
        Arguments.of("Fiat", "Uno", "Prata", null, PRICE, VehicleOrigin.OWNED, "O valor de entrada é obrigatório"),
        Arguments.of("Fiat", "Uno", "Prata", PRICE, null, VehicleOrigin.OWNED, "O preço de venda é obrigatório"),
        Arguments.of("Fiat", "Uno", "Prata", PRICE, PRICE, null, "A origem é obrigatória")
    );
  }

  static IntStream yearsOutOfRange() {
    return IntStream.of(1899, NEXT_YEAR + 1);
  }

  @Test
  void stripsTexts() {
    var details = new VehicleDetails("  Fiat ", " Uno ", 2020, " Prata ", PRICE, PRICE, VehicleOrigin.OWNED);

    assertThat(details.brand()).isEqualTo("Fiat");
    assertThat(details.model()).isEqualTo("Uno");
    assertThat(details.color()).isEqualTo("Prata");
  }

  @ParameterizedTest(name = "{6}")
  @MethodSource("missingRequiredFields")
  void rejectsMissingRequiredFields(
      String brand,
      String model,
      String color,
      Money purchasePrice,
      Money salePrice,
      VehicleOrigin origin,
      String expectedMessage
  ) {
    assertThatThrownBy(() -> new VehicleDetails(brand, model, 2020, color, purchasePrice, salePrice, origin))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void acceptsNextYear() {
    var details = new VehicleDetails("Fiat", "Uno", NEXT_YEAR, "Prata", PRICE, PRICE, VehicleOrigin.OWNED);

    assertThat(details.year()).isEqualTo(NEXT_YEAR);
  }

  @ParameterizedTest
  @MethodSource("yearsOutOfRange")
  void rejectsYearOutOfRange(int year) {
    assertThatThrownBy(() -> new VehicleDetails("Fiat", "Uno", year, "Prata", PRICE, PRICE, VehicleOrigin.OWNED))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void withPurchasePriceKeepsOtherFields() {
    var details = new VehicleDetails("Fiat", "Uno", 2020, "Prata", PRICE, Money.of("40000"), VehicleOrigin.OWNED);

    var readmitted = details.withPurchasePrice(Money.of("35000"));

    assertThat(readmitted).isEqualTo(
        new VehicleDetails("Fiat", "Uno", 2020, "Prata", Money.of("35000"), Money.of("40000"), VehicleOrigin.OWNED)
    );
  }
}

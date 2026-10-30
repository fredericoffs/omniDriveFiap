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

  private static final Plate PLATE = new Plate("ABC1D23");

  private static final int MILEAGE = 45000;

  private static final int NEXT_YEAR = Year.now(ZoneId.of("America/Sao_Paulo")).getValue() + 1;

  static Stream<Arguments> missingRequiredFields() {
    return Stream.of(
        Arguments.of(" ", "Uno", "Prata", PLATE, PRICE, PRICE, VehicleOrigin.OWNED, "A marca é obrigatória"),
        Arguments.of("Fiat", null, "Prata", PLATE, PRICE, PRICE, VehicleOrigin.OWNED, "O modelo é obrigatório"),
        Arguments.of("Fiat", "Uno", "", PLATE, PRICE, PRICE, VehicleOrigin.OWNED, "A cor é obrigatória"),
        Arguments.of("Fiat", "Uno", "Prata", null, PRICE, PRICE, VehicleOrigin.OWNED, "A placa é obrigatória"),
        Arguments.of("Fiat", "Uno", "Prata", PLATE, null, PRICE, VehicleOrigin.OWNED,
            "O valor de entrada é obrigatório"),
        Arguments.of("Fiat", "Uno", "Prata", PLATE, PRICE, null, VehicleOrigin.OWNED,
            "O preço de venda é obrigatório"),
        Arguments.of("Fiat", "Uno", "Prata", PLATE, PRICE, PRICE, null, "A origem é obrigatória")
    );
  }

  static IntStream yearsOutOfRange() {
    return IntStream.of(1899, NEXT_YEAR + 1);
  }

  private static VehicleDetails details(
      int year,
      int mileage
  ) {
    return new VehicleDetails(
        "Fiat",
        "Uno",
        year,
        "Prata",
        PLATE,
        mileage,
        PRICE,
        Money.of("40000"),
        VehicleOrigin.OWNED
    );
  }

  @Test
  void stripsTexts() {
    var details = new VehicleDetails(
        "  Fiat ",
        " Uno ",
        2020,
        " Prata ",
        PLATE,
        MILEAGE,
        PRICE,
        PRICE,
        VehicleOrigin.OWNED
    );

    assertThat(details.brand()).isEqualTo("Fiat");
    assertThat(details.model()).isEqualTo("Uno");
    assertThat(details.color()).isEqualTo("Prata");
  }

  @ParameterizedTest(name = "{7}")
  @MethodSource("missingRequiredFields")
  void rejectsMissingRequiredFields(
      String brand,
      String model,
      String color,
      Plate plate,
      Money purchasePrice,
      Money salePrice,
      VehicleOrigin origin,
      String expectedMessage
  ) {
    assertThatThrownBy(() -> new VehicleDetails(
        brand,
        model,
        2020,
        color,
        plate,
        MILEAGE,
        purchasePrice,
        salePrice,
        origin
    ))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(expectedMessage);
  }

  @Test
  void acceptsNextYear() {
    assertThat(details(NEXT_YEAR, MILEAGE).year()).isEqualTo(NEXT_YEAR);
  }

  @ParameterizedTest
  @MethodSource("yearsOutOfRange")
  void rejectsYearOutOfRange(int year) {
    assertThatThrownBy(() -> details(year, MILEAGE))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void acceptsZeroMileage() {
    assertThat(details(2020, 0).mileage()).isZero();
  }

  @Test
  void rejectsNegativeMileage() {
    assertThatThrownBy(() -> details(2020, -1))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("A quilometragem não pode ser negativa");
  }

  @Test
  void withReadmissionKeepsOtherFields() {
    var readmitted = details(2020, MILEAGE).withReadmission(Money.of("35000"), 52000);

    assertThat(readmitted).isEqualTo(new VehicleDetails(
        "Fiat",
        "Uno",
        2020,
        "Prata",
        PLATE,
        52000,
        Money.of("35000"),
        Money.of("40000"),
        VehicleOrigin.OWNED
    ));
  }
}

package br.com.fiap.omnidrive.domain.vehicle;

import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Chassi do veículo, gravado sem espaços nas pontas e em maiúsculas.
 *
 * <p>Com 17 caracteres, o chassi é tratado como VIN (ISO 3779): só letras e números, sem {@code I}, {@code O} e
 * {@code Q}. O Brasil adotou o VIN em 1986, e veículos até esse ano podem ter chassi em outro formato, aceito como
 * informado. A compatibilidade com o ano do veículo é conferida por {@link #requireCompatibleWith(int)}.
 *
 * <p>O dígito verificador (9ª posição) só é obrigatório na América do Norte, e muitas montadoras brasileiras não o
 * calculam. Por isso ele é apenas informado por {@link #hasValidCheckDigit()}, sem recusar o chassi.
 *
 * @param value o chassi, sem espaços nas pontas e em maiúsculas
 */
public record Chassis(String value) {

  /**
   * Último ano em que um veículo pode ter chassi fora do padrão VIN.
   */
  public static final int LAST_PRE_VIN_YEAR = 1986;

  /**
   * Primeiro ano-modelo em que a 10ª posição do VIN indica o ano-modelo (Resolução CONTRAN 24/1998). Antes, ela
   * indicava o ano de fabricação.
   */
  public static final int FIRST_MODEL_YEAR_CODE = 1999;

  private static final int MAX_LENGTH = 30;
  private static final int VIN_LENGTH = 17;
  private static final int CHECK_DIGIT_POSITION = 8;
  private static final int YEAR_CODE_POSITION = 9;
  private static final int YEAR_CODE_BASE = 1980;
  private static final String YEAR_CODES = "ABCDEFGHJKLMNPRSTVWXY123456789";
  private static final String LETTER_VALUES = "12345678012345070923456789";
  private static final int[] WEIGHTS = {8, 7, 6, 5, 4, 3, 2, 10, 0, 9, 8, 7, 6, 5, 4, 3, 2};
  private static final Pattern VIN_FORMAT = Pattern.compile("[A-HJ-NPR-Z0-9]{17}");

  public Chassis {
    if (value == null || value.isBlank()) {
      throw new BusinessRuleException("O chassi é obrigatório");
    }
    value = value.strip().toUpperCase(Locale.ROOT);
    if (value.length() > MAX_LENGTH) {
      throw new BusinessRuleException("O chassi deve ter no máximo " + MAX_LENGTH + " caracteres");
    }
    if (value.length() == VIN_LENGTH && !VIN_FORMAT.matcher(value).matches()) {
      throw new BusinessRuleException("O VIN deve conter só letras e números, sem I, O e Q");
    }
  }

  private static char yearCode(int year) {
    return YEAR_CODES.charAt(Math.floorMod(year - YEAR_CODE_BASE, YEAR_CODES.length()));
  }

  private static int transliterate(char character) {
    return Character.isDigit(character) ? character - '0' : LETTER_VALUES.charAt(character - 'A') - '0';
  }

  /**
   * Indica se o chassi segue o padrão VIN de 17 caracteres.
   */
  public boolean isVin() {
    return value.length() == VIN_LENGTH;
  }

  /**
   * Indica se o dígito verificador do VIN (9ª posição) confere com o cálculo da norma. Sempre {@code false} para um
   * chassi fora do padrão VIN.
   */
  public boolean hasValidCheckDigit() {
    if (!isVin()) {
      return false;
    }
    var sum = 0;
    for (var i = 0; i < VIN_LENGTH; i++) {
      sum += transliterate(value.charAt(i)) * WEIGHTS[i];
    }
    var remainder = sum % 11;
    var expected = remainder == 10 ? 'X' : (char) ('0' + remainder);
    return value.charAt(CHECK_DIGIT_POSITION) == expected;
  }

  /**
   * Confere se o chassi é compatível com o ano-modelo do veículo.
   *
   * <ul>
   *     <li>Fora do padrão VIN, só é aceito até {@value #LAST_PRE_VIN_YEAR}.</li>
   *     <li>No padrão VIN, o código da 10ª posição precisa ser o do ano-modelo. Antes de
   *     {@value #FIRST_MODEL_YEAR_CODE}, o código é do ano de fabricação, então também vale o do ano anterior. O
   *     código se repete a cada 30 anos, e por isso não identifica o ano sozinho.</li>
   * </ul>
   *
   * @throws BusinessRuleException se o chassi não for compatível com o ano
   */
  public void requireCompatibleWith(int modelYear) {
    if (!isVin()) {
      if (modelYear > LAST_PRE_VIN_YEAR) {
        throw new BusinessRuleException(
            "O chassi deve ser um VIN de 17 caracteres para veículos com ano a partir de " + (LAST_PRE_VIN_YEAR + 1));
      }
      return;
    }
    var code = value.charAt(YEAR_CODE_POSITION);
    var matchesModelYear = code == yearCode(modelYear);
    var matchesManufactureYear = modelYear < FIRST_MODEL_YEAR_CODE && code == yearCode(modelYear - 1);
    if (!matchesModelYear && !matchesManufactureYear) {
      throw new BusinessRuleException(
          "O ano " + modelYear + " não corresponde ao código de ano do VIN (10ª posição: " + code + ")");
    }
  }
}

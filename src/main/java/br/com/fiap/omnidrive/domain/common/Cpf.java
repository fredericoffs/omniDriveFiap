package br.com.fiap.omnidrive.domain.common;

import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;

/**
 * CPF válido, guardado só com os 11 dígitos. Aceita a entrada com ou sem pontuação ({@code 123.456.789-09} ou
 * {@code 12345678909}) e confere os dígitos verificadores.
 *
 * @param number os 11 dígitos do CPF, sem pontuação
 */
public record Cpf(String number) {

  private static final int LENGTH = 11;

  public Cpf {
    if (number == null || number.isBlank()) {
      throw new BusinessRuleException("O CPF é obrigatório");
    }
    if (!number.strip().matches("\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}")) {
      throw new BusinessRuleException("O CPF é inválido");
    }
    number = number.replaceAll("\\D", "");
    if (hasAllDigitsEqual(number) || !hasValidCheckDigits(number)) {
      throw new BusinessRuleException("O CPF é inválido");
    }
  }

  private static boolean hasAllDigitsEqual(String digits) {
    return digits.chars().distinct().count() == 1;
  }

  private static boolean hasValidCheckDigits(String digits) {
    return checkDigit(digits, LENGTH - 2) == digits.charAt(LENGTH - 2) - '0'
        && checkDigit(digits, LENGTH - 1) == digits.charAt(LENGTH - 1) - '0';
  }

  private static int checkDigit(
      String digits,
      int length
  ) {
    var sum = 0;
    for (var i = 0; i < length; i++) {
      sum += (digits.charAt(i) - '0') * (length + 1 - i);
    }
    var remainder = sum % LENGTH;
    return remainder < 2 ? 0 : LENGTH - remainder;
  }
}

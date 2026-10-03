package br.com.fiap.omnidrive.domain.vehicle;

import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Placa do veículo, no padrão antigo ({@code ABC-1234}) ou no Mercosul ({@code ABC1D23}). Aceita letras minúsculas
 * e o hífen, e guarda os 7 caracteres em maiúsculas, sem hífen.
 *
 * @param value os 7 caracteres da placa, em maiúsculas e sem hífen
 */
public record Plate(String value) {

  private static final Pattern OLD_FORMAT = Pattern.compile("[A-Z]{3}-?\\d{4}");
  private static final Pattern MERCOSUL_FORMAT = Pattern.compile("[A-Z]{3}\\d[A-Z]\\d{2}");

  public Plate {
    if (value == null || value.isBlank()) {
      throw new BusinessRuleException("A placa é obrigatória");
    }
    value = value.strip().toUpperCase(Locale.ROOT);
    if (!OLD_FORMAT.matcher(value).matches() && !MERCOSUL_FORMAT.matcher(value).matches()) {
      throw new BusinessRuleException("A placa deve seguir o padrão antigo (ABC-1234) ou o Mercosul (ABC1D23)");
    }
    value = value.replace("-", "");
  }
}

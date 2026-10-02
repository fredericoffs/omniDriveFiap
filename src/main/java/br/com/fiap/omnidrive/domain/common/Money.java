package br.com.fiap.omnidrive.domain.common;

import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;

import java.math.BigDecimal;

/**
 * Valor monetário em reais, sempre maior que zero e normalizado para duas casas decimais.
 *
 * @param amount valor em reais
 */
public record Money(BigDecimal amount) {

  public Money {
    if (amount == null) {
      throw new BusinessRuleException("O valor é obrigatório");
    }
    if (amount.signum() <= 0) {
      throw new BusinessRuleException("O valor deve ser maior que zero");
    }
    if (amount.stripTrailingZeros().scale() > 2) {
      throw new BusinessRuleException("O valor deve ter no máximo duas casas decimais");
    }
    amount = amount.setScale(2);
  }

  /**
   * Cria um valor a partir da representação textual, como {@code "1500.90"}.
   */
  public static Money of(String amount) {
    return new Money(new BigDecimal(amount));
  }
}

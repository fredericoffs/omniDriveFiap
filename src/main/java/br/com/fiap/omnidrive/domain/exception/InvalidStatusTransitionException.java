package br.com.fiap.omnidrive.domain.exception;

/**
 * Tentativa de mudar o status de uma entidade para um status que não é alcançável a partir do status atual.
 */
public class InvalidStatusTransitionException extends BusinessRuleException {

  public InvalidStatusTransitionException(String message) {
    super(message);
  }
}

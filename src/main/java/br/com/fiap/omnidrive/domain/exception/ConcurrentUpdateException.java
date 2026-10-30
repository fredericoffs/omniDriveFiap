package br.com.fiap.omnidrive.domain.exception;

/**
 * O registro foi alterado por outra operação entre a leitura e a gravação. A operação pode ser repetida a partir de
 * uma nova leitura.
 */
public class ConcurrentUpdateException extends RuntimeException {

  public ConcurrentUpdateException(String message) {
    super(message);
  }
}

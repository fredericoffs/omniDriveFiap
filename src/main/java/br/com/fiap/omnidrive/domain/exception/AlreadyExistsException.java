package br.com.fiap.omnidrive.domain.exception;

/**
 * Já existe um registro com um dado que deve ser único, como um veículo com o mesmo chassi ou a mesma placa.
 */
public class AlreadyExistsException extends RuntimeException {

  public AlreadyExistsException(String message) {
    super(message);
  }
}

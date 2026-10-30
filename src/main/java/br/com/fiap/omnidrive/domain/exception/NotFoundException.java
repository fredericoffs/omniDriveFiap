package br.com.fiap.omnidrive.domain.exception;

/**
 * O registro procurado não existe, como um veículo com o id informado.
 */
public class NotFoundException extends RuntimeException {

  public NotFoundException(String message) {
    super(message);
  }
}

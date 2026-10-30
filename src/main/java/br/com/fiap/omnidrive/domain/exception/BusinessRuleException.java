package br.com.fiap.omnidrive.domain.exception;

/**
 * Violação de uma regra de negócio do domínio, como um campo obrigatório ausente ou uma operação não permitida no status atual. A mensagem é escrita
 * para o cliente da API.
 */
public class BusinessRuleException extends RuntimeException {

  public BusinessRuleException(String message) {
    super(message);
  }
}

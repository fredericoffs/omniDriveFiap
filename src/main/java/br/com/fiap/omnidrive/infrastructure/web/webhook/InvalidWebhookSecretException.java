package br.com.fiap.omnidrive.infrastructure.web.webhook;

/**
 * Chamada ao webhook sem o segredo compartilhado ou com um segredo errado. Vira 401.
 */
public class InvalidWebhookSecretException extends RuntimeException {

  public InvalidWebhookSecretException() {
    super("Segredo do webhook ausente ou inválido");
  }
}

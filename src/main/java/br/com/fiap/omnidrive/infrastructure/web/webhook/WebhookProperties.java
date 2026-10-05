package br.com.fiap.omnidrive.infrastructure.web.webhook;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração do webhook de pagamento ({@code omnidrive.webhook.*}).
 *
 * @param secret segredo compartilhado com o processador de pagamento, enviado no header {@code X-Webhook-Secret}. Vem da
 *               variável de ambiente {@code WEBHOOK_SECRET} (Secret no Kubernetes); a aplicação não sobe com ele vazio
 */
@Validated
@ConfigurationProperties("omnidrive.webhook")
public record WebhookProperties(
    @NotBlank String secret
) {
}

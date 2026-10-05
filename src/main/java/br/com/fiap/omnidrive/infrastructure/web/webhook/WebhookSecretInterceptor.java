package br.com.fiap.omnidrive.infrastructure.web.webhook;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Confere o segredo compartilhado com o processador de pagamento antes de o webhook ser processado. A comparação é
 * feita em tempo constante, para não revelar pelo tempo de resposta quantos caracteres estão certos, e a resposta é a
 * mesma para o header ausente e para o valor errado.
 */
public class WebhookSecretInterceptor implements HandlerInterceptor {

  public static final String HEADER = "X-Webhook-Secret";
  public static final String SECURITY_SCHEME = "webhookSecret";

  private final byte[] expectedSecret;

  public WebhookSecretInterceptor(String secret) {
    this.expectedSecret = Objects.requireNonNull(secret).getBytes(StandardCharsets.UTF_8);
  }

  /**
   * @throws InvalidWebhookSecretException se o header não vier ou não tiver o segredo configurado
   */
  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler
  ) {
    var received = request.getHeader(HEADER);
    if (received == null || !MessageDigest.isEqual(expectedSecret, received.getBytes(StandardCharsets.UTF_8))) {
      throw new InvalidWebhookSecretException();
    }
    return true;
  }
}

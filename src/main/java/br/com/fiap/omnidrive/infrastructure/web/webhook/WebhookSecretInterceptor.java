package br.com.fiap.omnidrive.infrastructure.web.webhook;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Confere o segredo compartilhado com o processador de pagamento antes de o webhook ser processado. A comparação é
 * feita em tempo constante, para não revelar pelo tempo de resposta quantos caracteres estão certos, e a resposta é a
 * mesma para o header ausente e para o valor errado.
 *
 * <p>O preflight de CORS ({@code OPTIONS}) passa sem conferência: o navegador não envia headers personalizados nele, e
 * ele não chega ao controller. Como o webhook não tem CORS, o navegador recusa a chamada de qualquer forma.
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
    if (CorsUtils.isPreFlightRequest(request)) {
      return true;
    }
    var received = request.getHeader(HEADER);
    if (received == null || !MessageDigest.isEqual(expectedSecret, received.getBytes(StandardCharsets.UTF_8))) {
      throw new InvalidWebhookSecretException();
    }
    return true;
  }
}

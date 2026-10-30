package br.com.fiap.omnidrive.infrastructure.config;

import br.com.fiap.omnidrive.infrastructure.web.webhook.WebhookProperties;
import br.com.fiap.omnidrive.infrastructure.web.webhook.WebhookSecretInterceptor;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração do Spring MVC.
 *
 * <ul>
 *     <li>A conferência do segredo do webhook vale só para as rotas {@code /webhooks/**}.</li>
 *     <li>O CORS vale só para as rotas usadas pelo frontend ({@code /vehicles/**} e {@code /sales/**}), com as origens
 *     de {@link CorsProperties}. O webhook é chamado pelo servidor do processador de pagamento, e não por um
 *     navegador, então fica sem CORS. O header {@code Location} é exposto para o frontend ler a URL do recurso
 *     criado.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({WebhookProperties.class, CorsProperties.class})
public class WebConfig implements WebMvcConfigurer {

  private static final List<String> FRONTEND_PATHS = List.of("/vehicles/**", "/sales/**");
  private static final long PREFLIGHT_MAX_AGE_SECONDS = 3600;

  private final WebhookProperties webhookProperties;
  private final CorsProperties corsProperties;

  WebConfig(
      WebhookProperties webhookProperties,
      CorsProperties corsProperties
  ) {
    this.webhookProperties = webhookProperties;
    this.corsProperties = corsProperties;
  }

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(new WebhookSecretInterceptor(webhookProperties.secret()))
        .addPathPatterns("/webhooks/**");
  }

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {
    var origins = corsProperties.allowedOrigins().toArray(String[]::new);
    FRONTEND_PATHS.forEach(path -> registry.addMapping(path)
        .allowedOrigins(origins)
        .allowedMethods("GET", "POST", "PUT", "DELETE")
        .allowedHeaders(HttpHeaders.CONTENT_TYPE)
        .exposedHeaders(HttpHeaders.LOCATION)
        .maxAge(PREFLIGHT_MAX_AGE_SECONDS));
  }
}

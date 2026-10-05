package br.com.fiap.omnidrive.infrastructure.config;

import br.com.fiap.omnidrive.infrastructure.web.webhook.WebhookProperties;
import br.com.fiap.omnidrive.infrastructure.web.webhook.WebhookSecretInterceptor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração do Spring MVC: a conferência do segredo do webhook vale só para as rotas {@code /webhooks/**}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(WebhookProperties.class)
public class WebConfig implements WebMvcConfigurer {

  private final WebhookProperties webhookProperties;

  WebConfig(WebhookProperties webhookProperties) {
    this.webhookProperties = webhookProperties;
  }

  @Override
  public void addInterceptors(@NonNull InterceptorRegistry registry) {
    registry.addInterceptor(new WebhookSecretInterceptor(webhookProperties.secret()))
        .addPathPatterns("/webhooks/**");
  }
}

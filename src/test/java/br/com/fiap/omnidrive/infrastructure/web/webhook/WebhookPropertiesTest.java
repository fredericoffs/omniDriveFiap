package br.com.fiap.omnidrive.infrastructure.web.webhook;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Testes da ligação e da validação de {@link WebhookProperties}: a aplicação não sobe sem o segredo do webhook.
 */
class WebhookPropertiesTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
      .withUserConfiguration(PropertiesConfig.class);

  @Test
  void bindsSecret() {
    contextRunner
        .withPropertyValues("omnidrive.webhook.secret=segredo-de-teste")
        .run(context -> assertThat(context.getBean(WebhookProperties.class).secret()).isEqualTo("segredo-de-teste"));
  }

  @Test
  void failsToStartWithoutSecret() {
    contextRunner.run(context -> assertThat(context).hasFailed());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void failsToStartWithBlankSecret(String secret) {
    contextRunner
        .withPropertyValues("omnidrive.webhook.secret=" + secret)
        .run(context -> assertThat(context).hasFailed());
  }

  /**
   * Configuração mínima que registra só as propriedades do webhook.
   */
  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(WebhookProperties.class)
  static class PropertiesConfig {
  }
}

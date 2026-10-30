package br.com.fiap.omnidrive.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;

/**
 * Testes da ligação e da validação de {@link CorsProperties}: a lista de origens vem separada por vírgula, e a aplicação
 * não sobe sem ela.
 */
class CorsPropertiesTest {

  private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
      .withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
      .withUserConfiguration(PropertiesConfig.class);

  @Test
  void bindsCommaSeparatedOrigins() {
    contextRunner
        .withPropertyValues("omnidrive.cors.allowed-origins=https://loja.omnidrive.com,http://localhost:5173")
        .run(context -> assertThat(context.getBean(CorsProperties.class).allowedOrigins())
            .containsExactly("https://loja.omnidrive.com", "http://localhost:5173"));
  }

  @Test
  void failsToStartWithoutOrigins() {
    contextRunner.run(context -> assertThat(context).hasFailed());
  }

  @Test
  void failsToStartWithEmptyOrigins() {
    contextRunner
        .withPropertyValues("omnidrive.cors.allowed-origins=")
        .run(context -> assertThat(context).hasFailed());
  }

  /**
   * Configuração mínima que registra só as propriedades de CORS.
   */
  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties(CorsProperties.class)
  static class PropertiesConfig {
  }
}

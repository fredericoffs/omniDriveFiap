package br.com.fiap.omnidrive.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Informações gerais da documentação OpenAPI, exibidas no Swagger UI ({@code /swagger-ui.html}).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

  @Bean
  OpenAPI omniDriveOpenApi() {
    return new OpenAPI().info(
        new Info()
            .title("OmniDrive API")
            .version("v1")
            .description("""
                API da plataforma de revenda de veículos OmniDrive: cadastro, edição e listagem de veículos, \
                venda e recebimento do resultado do pagamento. Erros seguem o formato Problem Details \
                (RFC 9457).""")
    );
  }
}

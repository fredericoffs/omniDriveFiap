package br.com.fiap.omnidrive.infrastructure.config;

import br.com.fiap.omnidrive.infrastructure.web.webhook.WebhookSecretInterceptor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Informações gerais da documentação OpenAPI, exibidas no Swagger UI ({@code /swagger-ui.html}), e o esquema de
 * autenticação do webhook, aplicado só a ele.
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
    ).components(
        new Components().addSecuritySchemes(
            WebhookSecretInterceptor.SECURITY_SCHEME,
            new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(WebhookSecretInterceptor.HEADER)
                .description("Segredo compartilhado com o processador de pagamento, exigido só pelo webhook")
        )
    );
  }
}

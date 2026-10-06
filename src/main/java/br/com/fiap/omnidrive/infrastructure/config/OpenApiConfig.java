package br.com.fiap.omnidrive.infrastructure.config;

import br.com.fiap.omnidrive.infrastructure.web.webhook.WebhookSecretInterceptor;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/**
 * Informações gerais da documentação OpenAPI, exibidas no Swagger UI ({@code /swagger-ui.html}), o esquema de
 * autenticação do webhook, aplicado só a ele, e os ajustes no documento gerado: o tipo de conteúdo de cada resposta
 * ({@code application/json} no sucesso e {@code application/problem+json} nos erros) e o schema dos erros, que
 * descreve o {@link ProblemDetail} como ele é devolvido pela API.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

  private static final String ANY_MEDIA_TYPE = "*/*";
  private static final String PROBLEM_SCHEMA = ProblemDetail.class.getSimpleName();
  private static final String PROBLEM_SCHEMA_REF = "#/components/schemas/" + PROBLEM_SCHEMA;

  private static void fixMediaType(ApiResponse response) {
    var content = response.getContent();
    if (content == null || !content.containsKey(ANY_MEDIA_TYPE)) {
      return;
    }
    var mediaType = content.get(ANY_MEDIA_TYPE);
    var isProblem = PROBLEM_SCHEMA_REF.equals(mediaType.getSchema().get$ref());
    response.setContent(
        new Content().addMediaType(
            isProblem ? MediaType.APPLICATION_PROBLEM_JSON_VALUE : MediaType.APPLICATION_JSON_VALUE,
            mediaType
        )
    );
  }

  private static Schema<?> problemSchema() {
    return new JsonSchema()
        .types(Set.of("object"))
        .description("Erro no formato Problem Details (RFC 9457)")
        .required(List.of("title", "status", "detail", "instance"))
        .addProperty(
            "title",
            property(
                "string",
                "Resumo do tipo de erro",
                "Regra de negócio violada"
            )
        )
        .addProperty(
            "status",
            property(
                "integer",
                "Código de status HTTP",
                422
            )
        )
        .addProperty(
            "detail",
            property(
                "string",
                "Explicação do erro nesta requisição",
                "Não é possível mudar o status do veículo de RECEIVED para AVAILABLE"
            )
        )
        .addProperty(
            "instance",
            property(
                "string",
                "Caminho da requisição que gerou o erro",
                "/vehicles/0f8fad5b-d9cb-469f-a165-70867728950e/release"
            )
        )
        .addProperty(
            "errors",
            new JsonSchema()
                .types(Set.of("object"))
                .additionalProperties(new JsonSchema().types(Set.of("string")))
                .description("Mensagem de cada campo inválido, presente só no 400 de validação do corpo ou dos "
                    + "parâmetros de consulta")
                .example(Map.of("plate", "A placa é obrigatória"))
        );
  }

  private static Schema<?> property(
      String type,
      String description,
      Object example
  ) {
    return new JsonSchema()
        .types(Set.of(type))
        .description(description)
        .example(example);
  }

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

  @Bean
  OpenApiCustomizer responseContentCustomizer() {
    return openApi -> {
      openApi.getPaths().values().stream()
          .flatMap(path -> path.readOperations().stream())
          .flatMap(operation -> operation.getResponses().values().stream())
          .forEach(OpenApiConfig::fixMediaType);
      openApi.getComponents().addSchemas(PROBLEM_SCHEMA, problemSchema());
    };
  }
}

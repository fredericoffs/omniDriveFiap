package br.com.fiap.omnidrive.infrastructure.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração de CORS para o frontend ({@code omnidrive.cors.*}).
 *
 * @param allowedOrigins origens exatas que podem chamar a API pelo navegador, como {@code https://loja.omnidrive.com}.
 *                       Vem da variável de ambiente {@code CORS_ALLOWED_ORIGINS}, separada por vírgula (ConfigMap no
 *                       Kubernetes); a aplicação não sobe com a lista vazia
 */
@Validated
@ConfigurationProperties("omnidrive.cors")
public record CorsProperties(
    @NotEmpty List<String> allowedOrigins
) {
}

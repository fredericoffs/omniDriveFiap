package br.com.fiap.omnidrive;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Confere os endpoints de saúde do Actuator usados pelas probes de liveness e readiness do Kubernetes, com a aplicação
 * completa e o Postgres do Testcontainers.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ActuatorHealthIntegrationTest {

  @Autowired
  private MockMvcTester mvc;

  @ParameterizedTest
  @ValueSource(strings = {"/actuator/health", "/actuator/health/liveness", "/actuator/health/readiness"})
  void reportsApplicationAsUp(String path) {
    var response = mvc.get().uri(path).exchange();

    assertThat(response)
        .hasStatus(HttpStatus.OK)
        .bodyJson()
        .extractingPath("$.status")
        .isEqualTo("UP");
  }
}

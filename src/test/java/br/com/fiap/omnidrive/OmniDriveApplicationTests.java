package br.com.fiap.omnidrive;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Verifica se o contexto do Spring sobe contra o Postgres, com as migrations do Flyway aplicadas.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class OmniDriveApplicationTests {

  @Test
  void contextLoads() {
  }

}

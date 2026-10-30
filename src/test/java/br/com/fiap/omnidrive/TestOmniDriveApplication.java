package br.com.fiap.omnidrive;

import org.springframework.boot.SpringApplication;

/**
 * Roda a aplicação localmente com o Postgres do Testcontainers ({@code ./mvnw spring-boot:test-run}).
 */
public class TestOmniDriveApplication {

  static void main(String[] args) {
    SpringApplication.from(OmniDriveApplication::main).with(TestcontainersConfiguration.class).run(args);
  }

}

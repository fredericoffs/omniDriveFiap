package br.com.fiap.omnidrive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Ponto de entrada da API do OmniDrive, plataforma de revenda de veículos.
 */
@SpringBootApplication
public class OmniDriveApplication {

  public static void main(String[] args) {
    SpringApplication.run(OmniDriveApplication.class, args);
  }

}

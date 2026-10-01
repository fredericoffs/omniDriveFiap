package br.com.fiap.omnidrive;

import org.springframework.boot.SpringApplication;

public class TestOmniDriveApplication {

	public static void main(String[] args) {
		SpringApplication.from(OmniDriveApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}

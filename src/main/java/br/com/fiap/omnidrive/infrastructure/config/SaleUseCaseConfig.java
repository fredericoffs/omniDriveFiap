package br.com.fiap.omnidrive.infrastructure.config;

import br.com.fiap.omnidrive.application.gateway.PaymentGateway;
import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.application.gateway.TransactionRunner;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.application.usecase.CreateSale;
import br.com.fiap.omnidrive.application.usecase.GetSale;
import br.com.fiap.omnidrive.application.usecase.ProcessPaymentResult;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra os casos de uso de venda e de pagamento como beans. Os casos de uso são Java puro e não conhecem o Spring;
 * a ligação com a infraestrutura acontece só aqui.
 */
@Configuration(proxyBeanMethods = false)
public class SaleUseCaseConfig {

  @Bean
  CreateSale createSale(
      VehicleGateway vehicleGateway,
      SaleGateway saleGateway,
      PaymentGateway paymentGateway,
      TransactionRunner transactionRunner
  ) {
    return new CreateSale(
        vehicleGateway,
        saleGateway,
        paymentGateway,
        transactionRunner
    );
  }

  @Bean
  GetSale getSale(
      SaleGateway saleGateway,
      PaymentGateway paymentGateway
  ) {
    return new GetSale(
        saleGateway,
        paymentGateway
    );
  }

  @Bean
  ProcessPaymentResult processPaymentResult(
      PaymentGateway paymentGateway,
      SaleGateway saleGateway,
      VehicleGateway vehicleGateway,
      TransactionRunner transactionRunner
  ) {
    return new ProcessPaymentResult(
        paymentGateway,
        saleGateway,
        vehicleGateway,
        transactionRunner
    );
  }
}

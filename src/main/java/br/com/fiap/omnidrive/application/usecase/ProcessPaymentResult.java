package br.com.fiap.omnidrive.application.usecase;

import br.com.fiap.omnidrive.application.gateway.PaymentGateway;
import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.application.gateway.TransactionRunner;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: registrar o resultado de um pagamento informado pelo processador de pagamento (webhook), a partir do
 * código do pagamento.
 *
 * <p>O processador é a fonte da verdade: o pagamento muda conforme {@link Payment#register}, e o efeito no veículo
 * vem de {@link Payment#applyTo}, que só mexe no veículo se a venda for a venda atual dele. Uma repetição não grava
 * nada, então o processador pode reenviar a mesma notificação sem efeito duplicado.
 *
 * <p>O pagamento e o veículo, com o histórico, são gravados numa transação única.
 */
public class ProcessPaymentResult {

  private final PaymentGateway paymentGateway;
  private final SaleGateway saleGateway;
  private final VehicleGateway vehicleGateway;
  private final TransactionRunner transactionRunner;

  public ProcessPaymentResult(
      PaymentGateway paymentGateway,
      SaleGateway saleGateway,
      VehicleGateway vehicleGateway,
      TransactionRunner transactionRunner
  ) {
    this.paymentGateway = Objects.requireNonNull(paymentGateway);
    this.saleGateway = Objects.requireNonNull(saleGateway);
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
    this.transactionRunner = Objects.requireNonNull(transactionRunner);
  }

  /**
   * Registra o resultado e aplica o efeito dele no veículo da venda.
   *
   * @return o pagamento depois do registro, igual ao anterior se o resultado for uma repetição
   * @throws NotFoundException se não houver pagamento com o código informado
   * @throws InvalidStatusTransitionException se o resultado contradiz o status do pagamento, como um pagamento
   *     cancelado que recebe {@code PAID}
   * @throws ConcurrentUpdateException se o pagamento ou o veículo foram alterados por outra operação durante o
   *     registro, como uma notificação simultânea igual
   */
  public Payment execute(ProcessPaymentResultInput input) {
    Objects.requireNonNull(input);
    Objects.requireNonNull(input.paymentCode());
    Objects.requireNonNull(input.result());
    return transactionRunner.inTransaction(() -> {
      var payment = paymentGateway.findByPaymentCode(input.paymentCode())
          .orElseThrow(() -> new NotFoundException("Pagamento não encontrado: " + input.paymentCode()));
      if (!payment.register(input.result())) {
        return payment;
      }
      var savedPayment = paymentGateway.save(payment);
      var vehicle = vehicleOfSale(savedPayment.getSaleId());
      if (savedPayment.applyTo(vehicle)) {
        vehicleGateway.save(vehicle);
      }
      return savedPayment;
    });
  }

  private Vehicle vehicleOfSale(UUID saleId) {
    var sale = saleGateway.findById(saleId)
        .orElseThrow(() -> new IllegalStateException("Venda do pagamento não encontrada: " + saleId));
    return vehicleGateway.findById(sale.getVehicleId())
        .orElseThrow(() -> new IllegalStateException("Veículo da venda não encontrado: " + sale.getVehicleId()));
  }
}

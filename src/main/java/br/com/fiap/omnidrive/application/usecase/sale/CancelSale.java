package br.com.fiap.omnidrive.application.usecase.sale;

import br.com.fiap.omnidrive.application.gateway.PaymentGateway;
import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.application.gateway.TransactionRunner;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.sale.Payment;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso: cancelar uma venda a pedido da loja, enquanto o pagamento aguarda o resultado do processador.
 *
 * <p>O pagamento vai para Cancelado ({@link Payment#cancel()}) e o veículo volta de Reservado para Disponível pelo
 * mesmo efeito do webhook ({@link Payment#applyTo}). Cancelar de novo uma venda já cancelada não grava nada. O
 * pagamento e o veículo, com o histórico, são gravados numa transação única.
 */
public class CancelSale {

  private final SaleGateway saleGateway;
  private final PaymentGateway paymentGateway;
  private final VehicleGateway vehicleGateway;
  private final TransactionRunner transactionRunner;

  public CancelSale(
      SaleGateway saleGateway,
      PaymentGateway paymentGateway,
      VehicleGateway vehicleGateway,
      TransactionRunner transactionRunner
  ) {
    this.saleGateway = Objects.requireNonNull(saleGateway);
    this.paymentGateway = Objects.requireNonNull(paymentGateway);
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
    this.transactionRunner = Objects.requireNonNull(transactionRunner);
  }

  /**
   * Cancela a venda e libera a reserva do veículo.
   *
   * @return a venda com o pagamento cancelado
   * @throws NotFoundException se não houver venda com o id informado
   * @throws InvalidStatusTransitionException se o pagamento já estiver Pago, Recusado ou Estornado
   * @throws ConcurrentUpdateException se o pagamento ou o veículo foram alterados por outra operação durante o
   *     cancelamento, como um webhook simultâneo
   */
  public SaleWithPayment execute(UUID saleId) {
    Objects.requireNonNull(saleId);
    return transactionRunner.inTransaction(() -> {
      var sale = saleGateway.findById(saleId)
          .orElseThrow(() -> new NotFoundException("Venda não encontrada: " + saleId));
      var payment = paymentGateway.findBySaleId(saleId)
          .orElseThrow(() -> new IllegalStateException("Venda sem pagamento: " + saleId));
      if (!payment.cancel()) {
        return new SaleWithPayment(sale, payment);
      }
      var savedPayment = paymentGateway.save(payment);
      var vehicle = vehicleGateway.findById(sale.getVehicleId())
          .orElseThrow(() -> new IllegalStateException("Veículo da venda não encontrado: " + sale.getVehicleId()));
      if (savedPayment.applyTo(vehicle)) {
        vehicleGateway.save(vehicle);
      }
      return new SaleWithPayment(sale, savedPayment);
    });
  }
}

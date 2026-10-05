package br.com.fiap.omnidrive.application.usecase.sale;

import br.com.fiap.omnidrive.application.gateway.PaymentGateway;
import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.application.gateway.TransactionRunner;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.Sale;

import java.util.Objects;

/**
 * Caso de uso: efetuar a venda de um veículo Disponível. A venda copia o preço de venda do veículo, abre um pagamento
 * em Aguardando Pagamento e reserva o veículo, que passa a ter essa venda como atual.
 *
 * <p>Tudo é gravado numa transação única: a venda, o pagamento, o veículo e o histórico de status. A venda e o
 * pagamento são gravados antes do veículo, porque o veículo e o histórico apontam para a venda.
 */
public class CreateSale {

  private final VehicleGateway vehicleGateway;
  private final SaleGateway saleGateway;
  private final PaymentGateway paymentGateway;
  private final TransactionRunner transactionRunner;

  public CreateSale(
      VehicleGateway vehicleGateway,
      SaleGateway saleGateway,
      PaymentGateway paymentGateway,
      TransactionRunner transactionRunner
  ) {
    this.vehicleGateway = Objects.requireNonNull(vehicleGateway);
    this.saleGateway = Objects.requireNonNull(saleGateway);
    this.paymentGateway = Objects.requireNonNull(paymentGateway);
    this.transactionRunner = Objects.requireNonNull(transactionRunner);
  }

  /**
   * Efetua a venda e persiste a venda, o pagamento e a reserva do veículo.
   *
   * @return a venda e o pagamento aberto para ela
   * @throws NotFoundException se não houver veículo com o id informado
   * @throws BusinessRuleException se o CPF for inválido ou a data da venda faltar ou estiver no futuro
   * @throws InvalidStatusTransitionException se o veículo não estiver Disponível
   * @throws ConcurrentUpdateException se o veículo foi alterado por outra operação durante a venda, como uma venda
   *     simultânea
   */
  public SaleWithPayment execute(CreateSaleInput input) {
    Objects.requireNonNull(input);
    Objects.requireNonNull(input.vehicleId());
    return transactionRunner.inTransaction(() -> {
      var vehicle = vehicleGateway.findById(input.vehicleId())
          .orElseThrow(() -> new NotFoundException("Veículo não encontrado: " + input.vehicleId()));
      var sale = Sale.create(
          vehicle,
          new Cpf(input.buyerCpf()),
          input.saleDate()
      );
      vehicle.reserve(sale.getId());
      var savedSale = saleGateway.save(sale);
      var savedPayment = paymentGateway.save(Payment.open(savedSale.getId()));
      vehicleGateway.save(vehicle);
      return new SaleWithPayment(savedSale, savedPayment);
    });
  }
}

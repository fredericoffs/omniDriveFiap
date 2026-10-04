package br.com.fiap.omnidrive.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.PaymentGateway;
import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.application.gateway.TransactionRunner;
import br.com.fiap.omnidrive.application.gateway.VehicleGateway;
import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.PaymentResult;
import br.com.fiap.omnidrive.domain.sale.PaymentStatus;
import br.com.fiap.omnidrive.domain.sale.Sale;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link ProcessPaymentResult}.
 */
@ExtendWith(MockitoExtension.class)
class ProcessPaymentResultTest {

  private static final String PAYMENT_CODE = "3f2b8c1e-5d4a-4b6f-9e7d-1a2b3c4d5e6f";

  private static final UUID SALE_ID = UUID.randomUUID();

  private static final UUID VEHICLE_ID = UUID.randomUUID();

  @Mock
  private PaymentGateway paymentGateway;

  @Mock
  private SaleGateway saleGateway;

  @Mock
  private VehicleGateway vehicleGateway;

  @Mock
  private TransactionRunner transactionRunner;

  private ProcessPaymentResult processPaymentResult;

  private static Payment payment(PaymentStatus status) {
    return Payment.restore(
        UUID.randomUUID(),
        PAYMENT_CODE,
        SALE_ID,
        status,
        1
    );
  }

  private static Sale sale() {
    return Sale.restore(
        SALE_ID,
        VEHICLE_ID,
        new Cpf("52998224725"),
        LocalDate.of(2026, 10, 1),
        Money.of("40000")
    );
  }

  private static Vehicle vehicle(
      VehicleStatus status,
      UUID currentSaleId
  ) {
    return Vehicle.restore(
        VEHICLE_ID,
        new Chassis("9BWZZZ377LT004251"),
        new VehicleDetails(
            "Fiat",
            "Uno",
            2020,
            "Prata",
            new Plate("ABC1D23"),
            45000,
            Money.of("30000"),
            Money.of("40000"),
            VehicleOrigin.OWNED
        ),
        status,
        currentSaleId,
        3
    );
  }

  private static ProcessPaymentResultInput input(PaymentResult result) {
    return new ProcessPaymentResultInput(
        PAYMENT_CODE,
        result
    );
  }

  @BeforeEach
  void setUp() {
    processPaymentResult = new ProcessPaymentResult(paymentGateway, saleGateway, vehicleGateway, transactionRunner);
  }

  @ParameterizedTest
  @CsvSource({
      "AWAITING_PAYMENT, PAID,     PAID,     RESERVED, SOLD",
      "AWAITING_PAYMENT, DECLINED, DECLINED, RESERVED, AVAILABLE",
      "AWAITING_PAYMENT, CANCELED, CANCELED, RESERVED, AVAILABLE",
      "PAID,             CANCELED, REFUNDED, SOLD,     AVAILABLE"
  })
  void registersResultAndAppliesItToVehicleOfCurrentSale(
      PaymentStatus currentPayment,
      PaymentResult result,
      PaymentStatus expectedPayment,
      VehicleStatus currentVehicle,
      VehicleStatus expectedVehicle
  ) {
    runTransactions();
    var vehicle = vehicle(currentVehicle, SALE_ID);
    when(paymentGateway.findByPaymentCode(PAYMENT_CODE)).thenReturn(Optional.of(payment(currentPayment)));
    when(paymentGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(saleGateway.findById(SALE_ID)).thenReturn(Optional.of(sale()));
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

    var payment = processPaymentResult.execute(input(result));

    assertThat(payment.getStatus()).isEqualTo(expectedPayment);
    assertThat(vehicle.getStatus()).isEqualTo(expectedVehicle);
    var saves = inOrder(paymentGateway, vehicleGateway);
    saves.verify(paymentGateway).save(payment);
    saves.verify(vehicleGateway).save(vehicle);
  }

  @Test
  void changesOnlyPaymentWhenSaleIsNoLongerCurrent() {
    runTransactions();
    var readmitted = vehicle(VehicleStatus.RECEIVED, null);
    when(paymentGateway.findByPaymentCode(PAYMENT_CODE)).thenReturn(Optional.of(payment(PaymentStatus.PAID)));
    when(paymentGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(saleGateway.findById(SALE_ID)).thenReturn(Optional.of(sale()));
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(readmitted));

    var payment = processPaymentResult.execute(input(PaymentResult.CANCELED));

    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    assertThat(readmitted.getStatus()).isEqualTo(VehicleStatus.RECEIVED);
    verify(paymentGateway).save(payment);
    verify(vehicleGateway, never()).save(any());
  }

  @ParameterizedTest
  @CsvSource({
      "PAID,     PAID",
      "DECLINED, DECLINED",
      "CANCELED, CANCELED",
      "REFUNDED, CANCELED"
  })
  void savesNothingOnRepetition(
      PaymentStatus current,
      PaymentResult result
  ) {
    runTransactions();
    when(paymentGateway.findByPaymentCode(PAYMENT_CODE)).thenReturn(Optional.of(payment(current)));

    var payment = processPaymentResult.execute(input(result));

    assertThat(payment.getStatus()).isEqualTo(current);
    verify(paymentGateway, never()).save(any());
    verifyNoInteractions(saleGateway, vehicleGateway);
  }

  @Test
  void savesNothingWhenResultContradictsTerminalStatus() {
    runTransactions();
    when(paymentGateway.findByPaymentCode(PAYMENT_CODE)).thenReturn(Optional.of(payment(PaymentStatus.CANCELED)));
    var paidInput = input(PaymentResult.PAID);

    assertThatThrownBy(() -> processPaymentResult.execute(paidInput))
        .isInstanceOf(InvalidStatusTransitionException.class);
    verify(paymentGateway, never()).save(any());
    verifyNoInteractions(saleGateway, vehicleGateway);
  }

  @Test
  void failsWhenPaymentCodeDoesNotExist() {
    runTransactions();
    when(paymentGateway.findByPaymentCode(PAYMENT_CODE)).thenReturn(Optional.empty());
    var paidInput = input(PaymentResult.PAID);

    assertThatThrownBy(() -> processPaymentResult.execute(paidInput))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Pagamento não encontrado: " + PAYMENT_CODE);
    verifyNoInteractions(saleGateway, vehicleGateway);
  }

  @Test
  void accessesGatewaysOnlyInsideTransaction() {
    processPaymentResult.execute(input(PaymentResult.PAID));

    verify(transactionRunner).inTransaction(any());
    verifyNoInteractions(paymentGateway, saleGateway, vehicleGateway);
  }

  private void runTransactions() {
    when(transactionRunner.inTransaction(any()))
        .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
  }
}

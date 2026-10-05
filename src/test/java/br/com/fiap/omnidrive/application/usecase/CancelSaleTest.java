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
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link CancelSale}.
 */
@ExtendWith(MockitoExtension.class)
class CancelSaleTest {

  private static final UUID SALE_ID = UUID.randomUUID();

  private static final UUID VEHICLE_ID = UUID.randomUUID();

  @Mock
  private SaleGateway saleGateway;

  @Mock
  private PaymentGateway paymentGateway;

  @Mock
  private VehicleGateway vehicleGateway;

  @Mock
  private TransactionRunner transactionRunner;

  private CancelSale cancelSale;

  private static Sale sale() {
    return Sale.restore(
        SALE_ID,
        VEHICLE_ID,
        new Cpf("52998224725"),
        LocalDate.of(2026, 10, 1),
        Money.of("40000")
    );
  }

  private static Payment payment(PaymentStatus status) {
    return Payment.restore(
        UUID.randomUUID(),
        "3f2b8c1e-5d4a-4b6f-9e7d-1a2b3c4d5e6f",
        SALE_ID,
        status,
        1
    );
  }

  private static Vehicle reserved() {
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
        VehicleStatus.RESERVED,
        SALE_ID,
        3
    );
  }

  @BeforeEach
  void setUp() {
    cancelSale = new CancelSale(saleGateway, paymentGateway, vehicleGateway, transactionRunner);
  }

  @Test
  void cancelsPaymentAndReleasesVehicleReservation() {
    runTransactions();
    var vehicle = reserved();
    when(saleGateway.findById(SALE_ID)).thenReturn(Optional.of(sale()));
    when(paymentGateway.findBySaleId(SALE_ID)).thenReturn(Optional.of(payment(PaymentStatus.AWAITING_PAYMENT)));
    when(paymentGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));

    var result = cancelSale.execute(SALE_ID);

    assertThat(result.sale().getId()).isEqualTo(SALE_ID);
    assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.CANCELED);
    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    assertThat(vehicle.getCurrentSaleId()).isNull();
    var saves = inOrder(paymentGateway, vehicleGateway);
    saves.verify(paymentGateway).save(result.payment());
    saves.verify(vehicleGateway).save(vehicle);
  }

  @Test
  void savesNothingWhenSaleIsAlreadyCanceled() {
    runTransactions();
    when(saleGateway.findById(SALE_ID)).thenReturn(Optional.of(sale()));
    when(paymentGateway.findBySaleId(SALE_ID)).thenReturn(Optional.of(payment(PaymentStatus.CANCELED)));

    var result = cancelSale.execute(SALE_ID);

    assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.CANCELED);
    verify(paymentGateway, never()).save(any());
    verifyNoInteractions(vehicleGateway);
  }

  @ParameterizedTest
  @EnumSource(names = {"PAID", "DECLINED", "REFUNDED"})
  void savesNothingWhenPaymentAlreadyHasResult(PaymentStatus status) {
    runTransactions();
    when(saleGateway.findById(SALE_ID)).thenReturn(Optional.of(sale()));
    when(paymentGateway.findBySaleId(SALE_ID)).thenReturn(Optional.of(payment(status)));

    assertThatThrownBy(() -> cancelSale.execute(SALE_ID))
        .isInstanceOf(InvalidStatusTransitionException.class)
        .hasMessage("A venda não pode ser cancelada com o pagamento em " + status);
    verify(paymentGateway, never()).save(any());
    verifyNoInteractions(vehicleGateway);
  }

  @Test
  void failsWhenSaleDoesNotExist() {
    runTransactions();
    when(saleGateway.findById(SALE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> cancelSale.execute(SALE_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Venda não encontrada: " + SALE_ID);
    verifyNoInteractions(paymentGateway, vehicleGateway);
  }

  @Test
  void accessesGatewaysOnlyInsideTransaction() {
    cancelSale.execute(SALE_ID);

    verify(transactionRunner).inTransaction(any());
    verifyNoInteractions(saleGateway, paymentGateway, vehicleGateway);
  }

  private void runTransactions() {
    when(transactionRunner.inTransaction(any()))
        .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
  }
}

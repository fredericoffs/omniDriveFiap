package br.com.fiap.omnidrive.application.usecase.sale;

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
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.sale.PaymentStatus;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link CreateSale}.
 */
@ExtendWith(MockitoExtension.class)
class CreateSaleTest {

  private static final UUID VEHICLE_ID = UUID.randomUUID();

  private static final LocalDate TODAY = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

  private static final CreateSaleInput INPUT = new CreateSaleInput(
      VEHICLE_ID,
      "529.982.247-25",
      TODAY
  );

  @Mock
  private VehicleGateway vehicleGateway;

  @Mock
  private SaleGateway saleGateway;

  @Mock
  private PaymentGateway paymentGateway;

  @Mock
  private TransactionRunner transactionRunner;

  private CreateSale createSale;

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

  private static Stream<Arguments> unavailableVehicleArguments() {
    return Stream.of(
        Arguments.of(VehicleStatus.RECEIVED, null),
        Arguments.of(VehicleStatus.IN_PREPARATION, null),
        Arguments.of(VehicleStatus.RESERVED, UUID.randomUUID()),
        Arguments.of(VehicleStatus.SOLD, UUID.randomUUID())
    );
  }

  private static Stream<Arguments> invalidInputArguments() {
    return Stream.of(
        Arguments.of(null, TODAY, "O CPF é obrigatório"),
        Arguments.of("123.456.789-00", TODAY, "O CPF é inválido"),
        Arguments.of("529.982.247-25", null, "A data da venda é obrigatória"),
        Arguments.of("529.982.247-25", TODAY.plusDays(1), "A data da venda não pode estar no futuro")
    );
  }

  @BeforeEach
  void setUp() {
    createSale = new CreateSale(vehicleGateway, saleGateway, paymentGateway, transactionRunner);
  }

  @Test
  void createsSaleOpensPaymentAndReservesVehicle() {
    runTransactions();
    var vehicle = vehicle(VehicleStatus.AVAILABLE, null);
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle));
    when(saleGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(paymentGateway.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var result = createSale.execute(INPUT);

    var sale = result.sale();
    assertThat(sale.getVehicleId()).isEqualTo(VEHICLE_ID);
    assertThat(sale.getBuyerCpf()).isEqualTo(new Cpf("52998224725"));
    assertThat(sale.getSaleDate()).isEqualTo(TODAY);
    assertThat(sale.getAmount()).isEqualTo(Money.of("40000"));
    assertThat(result.payment().getSaleId()).isEqualTo(sale.getId());
    assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.AWAITING_PAYMENT);
    assertThat(result.payment().getPaymentCode()).isNotBlank();
    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RESERVED);
    assertThat(vehicle.isCurrentSale(sale.getId())).isTrue();
    assertThat(vehicle.getStatusChanges())
        .singleElement()
        .satisfies(history -> {
          assertThat(history.previousStatus()).isEqualTo(VehicleStatus.AVAILABLE);
          assertThat(history.newStatus()).isEqualTo(VehicleStatus.RESERVED);
          assertThat(history.saleId()).isEqualTo(sale.getId());
        });
    var saves = inOrder(saleGateway, paymentGateway, vehicleGateway);
    saves.verify(saleGateway).save(sale);
    saves.verify(paymentGateway).save(result.payment());
    saves.verify(vehicleGateway).save(vehicle);
  }

  @Test
  void accessesGatewaysOnlyInsideTransaction() {
    createSale.execute(INPUT);

    verify(transactionRunner).inTransaction(any());
    verifyNoInteractions(vehicleGateway, saleGateway, paymentGateway);
  }

  @Test
  void failsWhenVehicleDoesNotExist() {
    runTransactions();
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> createSale.execute(INPUT))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining(VEHICLE_ID.toString());
    verifyNothingSaved();
  }

  @ParameterizedTest
  @MethodSource("unavailableVehicleArguments")
  void doesNotSaveWhenVehicleIsNotAvailable(
      VehicleStatus status,
      UUID currentSaleId
  ) {
    runTransactions();
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(status, currentSaleId)));

    assertThatThrownBy(() -> createSale.execute(INPUT))
        .isInstanceOf(InvalidStatusTransitionException.class);
    verifyNothingSaved();
  }

  @ParameterizedTest
  @MethodSource("invalidInputArguments")
  void doesNotSaveWithInvalidInput(
      String buyerCpf,
      LocalDate saleDate,
      String expectedMessage
  ) {
    runTransactions();
    when(vehicleGateway.findById(VEHICLE_ID)).thenReturn(Optional.of(vehicle(VehicleStatus.AVAILABLE, null)));
    var invalidInput = new CreateSaleInput(
        VEHICLE_ID,
        buyerCpf,
        saleDate
    );

    assertThatThrownBy(() -> createSale.execute(invalidInput))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(expectedMessage);
    verifyNothingSaved();
  }

  private void runTransactions() {
    when(transactionRunner.inTransaction(any()))
        .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(0).get());
  }

  private void verifyNothingSaved() {
    verify(saleGateway, never()).save(any());
    verify(paymentGateway, never()).save(any());
    verify(vehicleGateway, never()).save(any());
  }
}

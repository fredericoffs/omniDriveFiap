package br.com.fiap.omnidrive.domain.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import br.com.fiap.omnidrive.domain.vehicle.Chassis;
import br.com.fiap.omnidrive.domain.vehicle.Plate;
import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleDetails;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testes da abertura de um {@link Payment}, do efeito de cada resultado do processador em cada status, do cancelamento
 * pela loja e do efeito de cada status no veículo.
 */
class PaymentTest {

  private static final UUID SALE_ID = UUID.randomUUID();

  private static Payment restore(PaymentStatus status) {
    return Payment.restore(
        UUID.randomUUID(),
        "code",
        SALE_ID,
        status,
        0
    );
  }

  private static Vehicle vehicle(
      VehicleStatus status,
      UUID currentSaleId
  ) {
    return Vehicle.restore(
        UUID.randomUUID(),
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

  private static Stream<Arguments> vehicleWithoutThisSaleArguments() {
    return Stream.of(
        Arguments.of(PaymentStatus.PAID, VehicleStatus.RESERVED, UUID.randomUUID()),
        Arguments.of(PaymentStatus.REFUNDED, VehicleStatus.SOLD, UUID.randomUUID()),
        Arguments.of(PaymentStatus.REFUNDED, VehicleStatus.RECEIVED, null),
        Arguments.of(PaymentStatus.CANCELED, VehicleStatus.AVAILABLE, null)
    );
  }

  @Test
  void opensAwaitingPaymentWithNewCode() {
    var payment = Payment.open(SALE_ID);
    var other = Payment.open(SALE_ID);

    assertThat(payment.getId()).isNotNull();
    assertThat(payment.getSaleId()).isEqualTo(SALE_ID);
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AWAITING_PAYMENT);
    assertThat(payment.getVersion()).isNull();
    assertThat(payment.getPaymentCode()).isNotBlank().isNotEqualTo(other.getPaymentCode());
  }

  @ParameterizedTest
  @CsvSource({
      "AWAITING_PAYMENT, PAID,     PAID",
      "AWAITING_PAYMENT, DECLINED, DECLINED",
      "AWAITING_PAYMENT, CANCELED, CANCELED",
      "PAID,             CANCELED, REFUNDED"
  })
  void changesStatus(
      PaymentStatus current,
      PaymentResult result,
      PaymentStatus expected
  ) {
    var payment = restore(current);

    assertThat(payment.register(result)).isTrue();
    assertThat(payment.getStatus()).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "PAID,     PAID",
      "DECLINED, DECLINED",
      "CANCELED, CANCELED",
      "REFUNDED, CANCELED"
  })
  void ignoresRepetition(
      PaymentStatus current,
      PaymentResult result
  ) {
    var payment = restore(current);

    assertThat(payment.register(result)).isFalse();
    assertThat(payment.getStatus()).isEqualTo(current);
  }

  @ParameterizedTest
  @CsvSource({
      "PAID,     DECLINED",
      "DECLINED, PAID",
      "DECLINED, CANCELED",
      "CANCELED, PAID",
      "CANCELED, DECLINED",
      "REFUNDED, PAID",
      "REFUNDED, DECLINED"
  })
  void rejectsResultThatContradictsStatus(
      PaymentStatus current,
      PaymentResult result
  ) {
    var payment = restore(current);

    assertThatThrownBy(() -> payment.register(result))
        .isInstanceOf(InvalidStatusTransitionException.class)
        .hasMessage("Não é possível registrar o resultado " + result + " em um pagamento com status " + current);
    assertThat(payment.getStatus()).isEqualTo(current);
  }

  @Test
  void storeCancelsPaymentAwaitingPayment() {
    var payment = restore(PaymentStatus.AWAITING_PAYMENT);

    assertThat(payment.cancel()).isTrue();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
  }

  @Test
  void ignoresRepeatedCancellation() {
    var payment = restore(PaymentStatus.CANCELED);

    assertThat(payment.cancel()).isFalse();
    assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
  }

  @ParameterizedTest
  @EnumSource(names = {"PAID", "DECLINED", "REFUNDED"})
  void rejectsCancellationAfterResult(PaymentStatus current) {
    var payment = restore(current);

    assertThatThrownBy(payment::cancel)
        .isInstanceOf(InvalidStatusTransitionException.class)
        .hasMessage("A venda não pode ser cancelada com o pagamento em " + current);
    assertThat(payment.getStatus()).isEqualTo(current);
  }

  @Test
  void storeCancellationReleasesReservationOfCurrentSale() {
    var payment = restore(PaymentStatus.AWAITING_PAYMENT);
    var vehicle = vehicle(VehicleStatus.RESERVED, SALE_ID);
    payment.cancel();

    assertThat(payment.applyTo(vehicle)).isTrue();
    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    assertThat(vehicle.getCurrentSaleId()).isNull();
  }

  @ParameterizedTest
  @CsvSource({
      "PAID,     RESERVED, SOLD,      true",
      "DECLINED, RESERVED, AVAILABLE, false",
      "CANCELED, RESERVED, AVAILABLE, false",
      "REFUNDED, SOLD,     AVAILABLE, false"
  })
  void appliesStatusToVehicleOfCurrentSale(
      PaymentStatus paymentStatus,
      VehicleStatus current,
      VehicleStatus expected,
      boolean keepsCurrentSale
  ) {
    var vehicle = vehicle(current, SALE_ID);

    assertThat(restore(paymentStatus).applyTo(vehicle)).isTrue();
    assertThat(vehicle.getStatus()).isEqualTo(expected);
    assertThat(vehicle.isCurrentSale(SALE_ID)).isEqualTo(keepsCurrentSale);
    assertThat(vehicle.getStatusChanges())
        .singleElement()
        .satisfies(history -> {
          assertThat(history.previousStatus()).isEqualTo(current);
          assertThat(history.newStatus()).isEqualTo(expected);
          assertThat(history.saleId()).isEqualTo(SALE_ID);
        });
  }

  @ParameterizedTest
  @MethodSource("vehicleWithoutThisSaleArguments")
  void doesNotChangeVehicleWhenSaleIsNotCurrent(
      PaymentStatus paymentStatus,
      VehicleStatus vehicleStatus,
      UUID currentSaleId
  ) {
    var vehicle = vehicle(vehicleStatus, currentSaleId);

    assertThat(restore(paymentStatus).applyTo(vehicle)).isFalse();
    assertThat(vehicle.getStatus()).isEqualTo(vehicleStatus);
    assertThat(vehicle.getStatusChanges()).isEmpty();
  }

  @Test
  void doesNotChangeVehicleWhileAwaitingPayment() {
    var vehicle = vehicle(VehicleStatus.RESERVED, SALE_ID);

    assertThat(restore(PaymentStatus.AWAITING_PAYMENT).applyTo(vehicle)).isFalse();
    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.RESERVED);
    assertThat(vehicle.getStatusChanges()).isEmpty();
  }

  @Test
  void rejectsEffectThatVehicleStatusDoesNotAllow() {
    var vehicle = vehicle(VehicleStatus.SOLD, SALE_ID);
    var payment = restore(PaymentStatus.PAID);

    assertThatThrownBy(() -> payment.applyTo(vehicle))
        .isInstanceOf(InvalidStatusTransitionException.class);
    assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.SOLD);
  }
}

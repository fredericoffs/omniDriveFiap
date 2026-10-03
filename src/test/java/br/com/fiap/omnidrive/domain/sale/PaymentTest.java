package br.com.fiap.omnidrive.domain.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.omnidrive.domain.exception.InvalidStatusTransitionException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Testes da abertura de um {@link Payment} e do efeito de cada resultado do processador em cada status.
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
}

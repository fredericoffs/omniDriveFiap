package br.com.fiap.omnidrive.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.fiap.omnidrive.application.gateway.PaymentGateway;
import br.com.fiap.omnidrive.application.gateway.SaleGateway;
import br.com.fiap.omnidrive.domain.common.Cpf;
import br.com.fiap.omnidrive.domain.common.Money;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.domain.sale.Payment;
import br.com.fiap.omnidrive.domain.sale.PaymentStatus;
import br.com.fiap.omnidrive.domain.sale.Sale;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Testes do caso de uso {@link GetSale}.
 */
@ExtendWith(MockitoExtension.class)
class GetSaleTest {

  private static final UUID SALE_ID = UUID.randomUUID();

  @Mock
  private SaleGateway saleGateway;

  @Mock
  private PaymentGateway paymentGateway;

  private GetSale getSale;

  private static Sale sale() {
    return Sale.restore(
        SALE_ID,
        UUID.randomUUID(),
        new Cpf("52998224725"),
        LocalDate.of(2026, 10, 1),
        Money.of("40000")
    );
  }

  private static Payment payment() {
    return Payment.restore(
        UUID.randomUUID(),
        "3f2b8c1e-5d4a-4b6f-9e7d-1a2b3c4d5e6f",
        SALE_ID,
        PaymentStatus.PAID,
        1
    );
  }

  @BeforeEach
  void setUp() {
    getSale = new GetSale(saleGateway, paymentGateway);
  }

  @Test
  void returnsSaleWithItsPayment() {
    var sale = sale();
    var payment = payment();
    when(saleGateway.findById(SALE_ID)).thenReturn(Optional.of(sale));
    when(paymentGateway.findBySaleId(SALE_ID)).thenReturn(Optional.of(payment));

    var result = getSale.execute(SALE_ID);

    assertThat(result).isEqualTo(new SaleWithPayment(sale, payment));
  }

  @Test
  void failsWhenSaleDoesNotExist() {
    when(saleGateway.findById(SALE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getSale.execute(SALE_ID))
        .isInstanceOf(NotFoundException.class)
        .hasMessage("Venda não encontrada: " + SALE_ID);
    verifyNoInteractions(paymentGateway);
  }

  @Test
  void failsWhenSaleHasNoPayment() {
    when(saleGateway.findById(SALE_ID)).thenReturn(Optional.of(sale()));
    when(paymentGateway.findBySaleId(SALE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> getSale.execute(SALE_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining(SALE_ID.toString());
  }
}

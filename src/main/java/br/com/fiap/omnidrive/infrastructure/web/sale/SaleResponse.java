package br.com.fiap.omnidrive.infrastructure.web.sale;

import br.com.fiap.omnidrive.application.usecase.sale.SaleWithPayment;
import br.com.fiap.omnidrive.domain.sale.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representação de uma venda e do seu pagamento nas respostas da API.
 */
@Schema(description = "Venda de um veículo, com o pagamento dela")
public record SaleResponse(

    @Schema(
        description = "Identificador da venda",
        example = "7c9e6679-7425-40de-944b-e07fc1f90ae7",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    UUID id,

    @Schema(
        description = "Identificador do veículo vendido",
        example = "0f8fad5b-d9cb-469f-a165-70867728950e",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    UUID vehicleId,

    @Schema(
        description = "CPF do comprador, só com os 11 dígitos",
        example = "52998224725",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String buyerCpf,

    @Schema(description = "Data da venda", example = "2026-10-04", requiredMode = Schema.RequiredMode.REQUIRED)
    LocalDate saleDate,

    @Schema(
        description = "Valor da venda, copiado do preço de venda do veículo no momento da venda",
        example = "40000.00",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    BigDecimal amount,

    @Schema(
        description = "Código do pagamento. É com ele que o processador de pagamento informa o resultado pelo webhook",
        example = "3f2b8c1e-5d4a-4b6f-9e7d-1a2b3c4d5e6f",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String paymentCode,

    @Schema(
        description = "Status do pagamento: AWAITING_PAYMENT (aguardando pagamento), PAID (pago), DECLINED "
            + "(recusado), CANCELED (cancelado) ou REFUNDED (estornado)",
        example = "AWAITING_PAYMENT",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    PaymentStatus paymentStatus
) {

  static SaleResponse from(SaleWithPayment result) {
    var sale = result.sale();
    var payment = result.payment();
    return new SaleResponse(
        sale.getId(),
        sale.getVehicleId(),
        sale.getBuyerCpf().number(),
        sale.getSaleDate(),
        sale.getAmount().amount(),
        payment.getPaymentCode(),
        payment.getStatus()
    );
  }
}

package br.com.fiap.omnidrive.infrastructure.web.sale;

import br.com.fiap.omnidrive.application.usecase.sale.CreateSaleInput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Corpo da requisição de venda de veículo.
 */
@Schema(description = "Dados para efetuar a venda de um veículo")
public record CreateSaleRequest(

    @Schema(
        description = "Identificador do veículo vendido. Ele precisa estar com status AVAILABLE (disponível)",
        example = "0f8fad5b-d9cb-469f-a165-70867728950e"
    )
    @NotNull(message = "O veículo é obrigatório")
    UUID vehicleId,

    @Schema(description = "CPF do comprador, com ou sem pontuação", example = "529.982.247-25")
    @NotBlank(message = "O CPF do comprador é obrigatório")
    String buyerCpf,

    @Schema(
        description = "Data da venda, no formato AAAA-MM-DD. Pode estar no passado, mas não no futuro",
        example = "2026-10-04"
    )
    @NotNull(message = "A data da venda é obrigatória")
    LocalDate saleDate
) {

  CreateSaleInput toInput() {
    return new CreateSaleInput(
        vehicleId,
        buyerCpf,
        saleDate
    );
  }
}

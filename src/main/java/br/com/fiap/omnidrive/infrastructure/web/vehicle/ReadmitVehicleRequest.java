package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.ReadmitVehicleInput;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/**
 * Corpo da requisição de reentrada de veículo.
 */
@Schema(description = "Dados para registrar a volta de um veículo vendido à loja")
public record ReadmitVehicleRequest(

    @Schema(
        description = "Novo valor de entrada: quanto a loja pagou para recomprar o veículo, ou o valor acordado com o "
            + "dono se consignado",
        example = "35000.00"
    )
    @NotNull(message = "O valor de entrada é obrigatório")
    @Positive(message = "O valor de entrada deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "O valor de entrada deve ter até 10 dígitos inteiros e 2 decimais")
    BigDecimal purchasePrice,

    @Schema(description = "Quilometragem na volta, em km. Deve ser maior que a quilometragem atual", example = "52000")
    @NotNull(message = "A quilometragem é obrigatória")
    @PositiveOrZero(message = "A quilometragem não pode ser negativa")
    Integer mileage
) {

  ReadmitVehicleInput toInput() {
    return new ReadmitVehicleInput(
        purchasePrice,
        mileage
    );
  }
}

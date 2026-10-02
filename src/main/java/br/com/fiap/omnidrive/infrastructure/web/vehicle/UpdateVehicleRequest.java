package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.UpdateVehicleInput;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Corpo da requisição de edição de veículo. Todos os campos são substituídos.
 */
@Schema(description = "Novos dados do veículo. Todos os campos são substituídos")
public record UpdateVehicleRequest(

    @Schema(description = "Marca", example = "Fiat", maxLength = 60)
    @NotBlank(message = "A marca é obrigatória")
    @Size(max = 60, message = "A marca deve ter no máximo 60 caracteres")
    String brand,

    @Schema(description = "Modelo", example = "Uno", maxLength = 60)
    @NotBlank(message = "O modelo é obrigatório")
    @Size(max = 60, message = "O modelo deve ter no máximo 60 caracteres")
    String model,

    @Schema(description = "Ano do veículo, entre 1900 e o ano seguinte ao atual", example = "2020")
    @NotNull(message = "O ano é obrigatório")
    Integer year,

    @Schema(description = "Cor", example = "Prata", maxLength = 30)
    @NotBlank(message = "A cor é obrigatória")
    @Size(max = 30, message = "A cor deve ter no máximo 30 caracteres")
    String color,

    @Schema(description = "Valor de entrada", example = "30000.00")
    @NotNull(message = "O valor de entrada é obrigatório")
    @Positive(message = "O valor de entrada deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "O valor de entrada deve ter até 10 dígitos inteiros e 2 decimais")
    BigDecimal purchasePrice,

    @Schema(description = "Preço de venda anunciado", example = "38500.00")
    @NotNull(message = "O preço de venda é obrigatório")
    @Positive(message = "O preço de venda deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "O preço de venda deve ter até 10 dígitos inteiros e 2 decimais")
    BigDecimal salePrice,

    @Schema(description = "Origem: OWNED (próprio da loja) ou CONSIGNED (consignado)", example = "OWNED")
    @NotNull(message = "A origem é obrigatória")
    VehicleOrigin origin,

    @Schema(
        description = "Versão do veículo lida antes da edição (campo version da resposta). Se o veículo mudou desde "
            + "então, a edição é recusada com 409",
        example = "0"
    )
    @NotNull(message = "A versão do veículo é obrigatória")
    Long version
) {

  UpdateVehicleInput toInput() {
    return new UpdateVehicleInput(brand, model, year, color, purchasePrice, salePrice, origin, version);
  }
}

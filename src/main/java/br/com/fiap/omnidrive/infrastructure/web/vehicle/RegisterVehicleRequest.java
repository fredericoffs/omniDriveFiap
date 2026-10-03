package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.RegisterVehicleInput;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Corpo da requisição de cadastro de veículo.
 */
@Schema(description = "Dados para cadastrar um veículo que chegou à loja")
public record RegisterVehicleRequest(

    @Schema(
        description = "Chassi. A partir de 1987, deve ser um VIN de 17 caracteres (sem I, O e Q), com o código da "
            + "10ª posição correspondente ao ano; até 1986, outros formatos são aceitos. É único, não pode ser "
            + "alterado depois do cadastro e é gravado em maiúsculas",
        example = "9BWZZZ377LT004251",
        maxLength = 30
    )
    @NotBlank(message = "O chassi é obrigatório")
    @Size(max = 30, message = "O chassi deve ter no máximo 30 caracteres")
    String chassis,

    @Schema(
        description = "Placa, no padrão antigo (ABC-1234) ou no Mercosul (ABC1D23). Letras minúsculas e hífen são "
            + "aceitos; a placa é gravada em maiúsculas e sem hífen",
        example = "ABC1D23"
    )
    @NotBlank(message = "A placa é obrigatória")
    String plate,

    @Schema(description = "Quilometragem, em km", example = "45000")
    @NotNull(message = "A quilometragem é obrigatória")
    @PositiveOrZero(message = "A quilometragem não pode ser negativa")
    Integer mileage,

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

    @Schema(
        description = "Valor de entrada: quanto a loja pagou pelo veículo, ou o valor acordado com o dono se consignado",
        example = "30000.00"
    )
    @NotNull(message = "O valor de entrada é obrigatório")
    @Positive(message = "O valor de entrada deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "O valor de entrada deve ter até 10 dígitos inteiros e 2 decimais")
    BigDecimal purchasePrice,

    @Schema(description = "Preço de venda anunciado", example = "40000.00")
    @NotNull(message = "O preço de venda é obrigatório")
    @Positive(message = "O preço de venda deve ser maior que zero")
    @Digits(integer = 10, fraction = 2, message = "O preço de venda deve ter até 10 dígitos inteiros e 2 decimais")
    BigDecimal salePrice,

    @Schema(description = "Origem: OWNED (próprio da loja) ou CONSIGNED (consignado)", example = "OWNED")
    @NotNull(message = "A origem é obrigatória")
    VehicleOrigin origin
) {

  RegisterVehicleInput toInput() {
    return new RegisterVehicleInput(
        chassis,
        plate,
        mileage,
        brand,
        model,
        year,
        color,
        purchasePrice,
        salePrice,
        origin
    );
  }
}

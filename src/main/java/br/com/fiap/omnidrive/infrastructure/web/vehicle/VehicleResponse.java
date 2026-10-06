package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.domain.vehicle.Vehicle;
import br.com.fiap.omnidrive.domain.vehicle.VehicleOrigin;
import br.com.fiap.omnidrive.domain.vehicle.VehicleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Representação de um veículo nas respostas da API.
 */
@Schema(description = "Veículo da loja")
public record VehicleResponse(

    @Schema(
        description = "Identificador do veículo",
        example = "0f8fad5b-d9cb-469f-a165-70867728950e",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    UUID id,

    @Schema(
        description = "Chassi, em maiúsculas",
        example = "9BWZZZ377LT004251",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String chassis,

    @Schema(
        description = "Indica se o chassi segue o padrão VIN de 17 caracteres. Veículos até 1986 podem ter chassi "
            + "em outro formato",
        example = "true",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    boolean vin,

    @Schema(
        description = "Indica se o dígito verificador do VIN (9ª posição) confere. É só informativo: o dígito é "
            + "obrigatório na América do Norte, mas muitas montadoras brasileiras não o calculam. Nulo quando o "
            + "chassi não é VIN",
        example = "false",
        nullable = true,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    Boolean vinCheckDigitValid,

    @Schema(
        description = "Placa, em maiúsculas e sem hífen",
        example = "ABC1D23",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String plate,

    @Schema(description = "Quilometragem, em km", example = "45000", requiredMode = Schema.RequiredMode.REQUIRED)
    int mileage,

    @Schema(description = "Marca", example = "Fiat", requiredMode = Schema.RequiredMode.REQUIRED)
    String brand,

    @Schema(description = "Modelo", example = "Uno", requiredMode = Schema.RequiredMode.REQUIRED)
    String model,

    @Schema(description = "Ano", example = "2020", requiredMode = Schema.RequiredMode.REQUIRED)
    int year,

    @Schema(description = "Cor", example = "Prata", requiredMode = Schema.RequiredMode.REQUIRED)
    String color,

    @Schema(description = "Valor de entrada", example = "30000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal purchasePrice,

    @Schema(description = "Preço de venda anunciado", example = "40000.00", requiredMode = Schema.RequiredMode.REQUIRED)
    BigDecimal salePrice,

    @Schema(
        description = "Origem: OWNED (próprio da loja) ou CONSIGNED (consignado)",
        example = "OWNED",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    VehicleOrigin origin,

    @Schema(
        description = "Status: RECEIVED (recebido), IN_PREPARATION (em preparação), AVAILABLE (disponível), "
            + "RESERVED (reservado), SOLD (vendido) ou RETURNED_TO_OWNER (devolvido ao dono)",
        example = "RECEIVED",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    VehicleStatus status,

    @Schema(
        description = "Versão do veículo. Deve ser enviada na edição",
        example = "0",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    long version
) {

  static VehicleResponse from(Vehicle vehicle) {
    return new VehicleResponse(
        vehicle.getId(),
        vehicle.getChassis().value(),
        vehicle.getChassis().isVin(),
        vehicle.getChassis().isVin() ? vehicle.getChassis().hasValidCheckDigit() : null,
        vehicle.getPlate().value(),
        vehicle.getMileage(),
        vehicle.getBrand(),
        vehicle.getModel(),
        vehicle.getYear(),
        vehicle.getColor(),
        vehicle.getPurchasePrice().amount(),
        vehicle.getSalePrice().amount(),
        vehicle.getOrigin(),
        vehicle.getStatus(),
        vehicle.getVersion()
    );
  }
}

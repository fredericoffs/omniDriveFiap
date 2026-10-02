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

    @Schema(description = "Identificador do veículo", example = "0f8fad5b-d9cb-469f-a165-70867728950e")
    UUID id,

    @Schema(description = "Marca", example = "Fiat")
    String brand,

    @Schema(description = "Modelo", example = "Uno")
    String model,

    @Schema(description = "Ano", example = "2020")
    int year,

    @Schema(description = "Cor", example = "Prata")
    String color,

    @Schema(description = "Valor de entrada", example = "30000.00")
    BigDecimal purchasePrice,

    @Schema(description = "Preço de venda anunciado", example = "40000.00")
    BigDecimal salePrice,

    @Schema(description = "Origem: OWNED (próprio da loja) ou CONSIGNED (consignado)", example = "OWNED")
    VehicleOrigin origin,

    @Schema(
        description = "Status: RECEIVED (recebido), IN_PREPARATION (em preparação), AVAILABLE (disponível), "
            + "RESERVED (reservado) ou SOLD (vendido)",
        example = "RECEIVED"
    )
    VehicleStatus status,

    @Schema(description = "Versão do veículo. Deve ser enviada na edição", example = "0")
    long version
) {

  static VehicleResponse from(Vehicle vehicle) {
    return new VehicleResponse(
        vehicle.getId(),
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

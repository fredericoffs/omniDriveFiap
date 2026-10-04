package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.SoldVehicle;
import br.com.fiap.omnidrive.domain.sale.Sale;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Representação de um veículo vendido, com a venda atual dele, na listagem de vendidos.
 */
@Schema(description = "Veículo vendido, com a venda atual")
public record SoldVehicleResponse(

    @Schema(description = "Dados do veículo")
    VehicleResponse vehicle,

    @Schema(description = "Venda atual do veículo")
    SaleSummary sale
) {

  static SoldVehicleResponse from(SoldVehicle soldVehicle) {
    return new SoldVehicleResponse(
        VehicleResponse.from(soldVehicle.vehicle()),
        SaleSummary.from(soldVehicle.sale())
    );
  }

  /**
   * Dados da venda atual de um veículo vendido.
   */
  @Schema(description = "Venda atual do veículo")
  public record SaleSummary(

      @Schema(description = "Identificador da venda", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
      UUID id,

      @Schema(description = "Data da venda", example = "2026-10-04")
      LocalDate saleDate,

      @Schema(description = "Valor da venda, que define a ordem da listagem", example = "40000.00")
      BigDecimal amount,

      @Schema(description = "CPF do comprador, só com os 11 dígitos", example = "52998224725")
      String buyerCpf
  ) {

    static SaleSummary from(Sale sale) {
      return new SaleSummary(
          sale.getId(),
          sale.getSaleDate(),
          sale.getAmount().amount(),
          sale.getBuyerCpf().number()
      );
    }
  }
}

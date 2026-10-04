package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.ListSoldVehicles;
import br.com.fiap.omnidrive.application.usecase.ListVehiclesForSale;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de listagem de veículos: os veículos à venda e os vendidos.
 */
@Tag(name = "Veículos", description = "Cadastro, consulta, edição, preparação, reentrada e listagens de veículos")
@RestController
@RequestMapping("/vehicles")
public class VehicleListingController {

  private final ListVehiclesForSale listVehiclesForSale;
  private final ListSoldVehicles listSoldVehicles;

  VehicleListingController(
      ListVehiclesForSale listVehiclesForSale,
      ListSoldVehicles listSoldVehicles
  ) {
    this.listVehiclesForSale = listVehiclesForSale;
    this.listSoldVehicles = listSoldVehicles;
  }

  @Operation(
      summary = "Listar veículos à venda",
      description = "Lista os veículos com status AVAILABLE (disponível), ordenados pelo preço de venda, do menor "
          + "para o maior."
  )
  @ApiResponse(responseCode = "200", description = "Veículos à venda; lista vazia se não houver nenhum")
  @GetMapping("/for-sale")
  List<VehicleResponse> listForSale() {
    return listVehiclesForSale.execute().stream()
        .map(VehicleResponse::from)
        .toList();
  }

  @Operation(
      summary = "Listar veículos vendidos",
      description = """
          Lista os veículos com status SOLD (vendido), cada um com a venda atual, ordenados pelo valor da venda, \
          do menor para o maior. Um veículo que voltou à loja pela reentrada sai da listagem."""
  )
  @ApiResponse(responseCode = "200", description = "Veículos vendidos; lista vazia se não houver nenhum")
  @GetMapping("/sold")
  List<SoldVehicleResponse> listSold() {
    return listSoldVehicles.execute().stream()
        .map(SoldVehicleResponse::from)
        .toList();
  }
}

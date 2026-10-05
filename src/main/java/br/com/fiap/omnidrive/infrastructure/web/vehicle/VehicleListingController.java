package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.ListSoldVehicles;
import br.com.fiap.omnidrive.application.usecase.ListVehiclesForSale;
import br.com.fiap.omnidrive.infrastructure.web.PageParams;
import br.com.fiap.omnidrive.infrastructure.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de listagem de veículos: os veículos à venda e os vendidos. As duas listagens são paginadas.
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
      description = """
          Lista os veículos com status AVAILABLE (disponível), ordenados pelo preço de venda, do menor para o \
          maior. Preços iguais são desempatados pelo id, para que a ordem não mude entre as páginas."""
  )
  @ApiResponse(responseCode = "200", description = "Página dos veículos à venda; conteúdo vazio se não houver nenhum")
  @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos")
  @GetMapping("/for-sale")
  PageResponse<VehicleResponse> listForSale(@ParameterObject @Valid PageParams pageParams) {
    return PageResponse.from(
        listVehiclesForSale.execute(pageParams.toPageQuery()),
        VehicleResponse::from
    );
  }

  @Operation(
      summary = "Listar veículos vendidos",
      description = """
          Lista os veículos com status SOLD (vendido), cada um com a venda atual, ordenados pelo valor da venda, \
          do menor para o maior. Valores iguais são desempatados pelo id do veículo, para que a ordem não mude \
          entre as páginas. Um veículo que voltou à loja pela reentrada sai da listagem."""
  )
  @ApiResponse(responseCode = "200", description = "Página dos veículos vendidos; conteúdo vazio se não houver nenhum")
  @ApiResponse(responseCode = "400", description = "Parâmetros de paginação inválidos")
  @GetMapping("/sold")
  PageResponse<SoldVehicleResponse> listSold(@ParameterObject @Valid PageParams pageParams) {
    return PageResponse.from(
        listSoldVehicles.execute(pageParams.toPageQuery()),
        SoldVehicleResponse::from
    );
  }
}

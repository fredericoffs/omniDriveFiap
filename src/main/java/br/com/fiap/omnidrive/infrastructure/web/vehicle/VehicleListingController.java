package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.sale.ListSoldVehicles;
import br.com.fiap.omnidrive.application.usecase.vehicle.ListVehiclesForSale;
import br.com.fiap.omnidrive.application.usecase.vehicle.SearchVehicles;
import br.com.fiap.omnidrive.infrastructure.web.PageParams;
import br.com.fiap.omnidrive.infrastructure.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de listagem de veículos: a busca com filtros, os veículos à venda e os vendidos. Todas as listagens são
 * paginadas.
 */
@Tag(name = "Veículos", description = "Cadastro, consulta, edição, exclusão, mudanças de status e listagens de veículos")
@RestController
@RequestMapping("/vehicles")
public class VehicleListingController {

  private final SearchVehicles searchVehicles;
  private final ListVehiclesForSale listVehiclesForSale;
  private final ListSoldVehicles listSoldVehicles;

  VehicleListingController(
      SearchVehicles searchVehicles,
      ListVehiclesForSale listVehiclesForSale,
      ListSoldVehicles listSoldVehicles
  ) {
    this.searchVehicles = searchVehicles;
    this.listVehiclesForSale = listVehiclesForSale;
    this.listSoldVehicles = listSoldVehicles;
  }

  @Operation(
      operationId = "searchVehicles",
      summary = "Buscar veículos",
      description = """
          Busca veículos em qualquer status, combinando os filtros informados, todos opcionais. Placa e chassi são \
          comparados pelo valor exato, depois da mesma normalização do cadastro; marca e modelo, por trecho, sem \
          diferenciar maiúsculas de minúsculas. O resultado vem do cadastro mais recente para o mais antigo, com \
          desempate pelo id. Sem filtros, lista todos os veículos."""
  )
  @ApiResponse(
      responseCode = "200",
      description = "Página dos veículos encontrados; conteúdo vazio se nenhum atender aos filtros"
  )
  @ApiResponse(
      responseCode = "400",
      description = "Status desconhecido ou parâmetros de paginação inválidos",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "Placa ou chassi fora do formato aceito no cadastro",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @GetMapping
  PageResponse<VehicleResponse> search(
      @ParameterObject SearchVehiclesParams searchParams,
      @ParameterObject @Valid PageParams pageParams
  ) {
    return PageResponse.from(
        searchVehicles.execute(searchParams.toInput(), pageParams.toPageQuery()),
        VehicleResponse::from
    );
  }

  @Operation(
      operationId = "listVehiclesForSale",
      summary = "Listar veículos à venda",
      description = """
          Lista os veículos com status AVAILABLE (disponível), ordenados pelo preço de venda, do menor para o \
          maior. Preços iguais são desempatados pelo id, para que a ordem não mude entre as páginas."""
  )
  @ApiResponse(responseCode = "200", description = "Página dos veículos à venda; conteúdo vazio se não houver nenhum")
  @ApiResponse(
      responseCode = "400",
      description = "Parâmetros de paginação inválidos",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @GetMapping("/for-sale")
  PageResponse<VehicleResponse> listForSale(@ParameterObject @Valid PageParams pageParams) {
    return PageResponse.from(
        listVehiclesForSale.execute(pageParams.toPageQuery()),
        VehicleResponse::from
    );
  }

  @Operation(
      operationId = "listSoldVehicles",
      summary = "Listar veículos vendidos",
      description = """
          Lista os veículos com status SOLD (vendido), cada um com a venda atual, ordenados pelo valor da venda, \
          do menor para o maior. Valores iguais são desempatados pelo id do veículo, para que a ordem não mude \
          entre as páginas. Um veículo que voltou à loja pela reentrada sai da listagem."""
  )
  @ApiResponse(responseCode = "200", description = "Página dos veículos vendidos; conteúdo vazio se não houver nenhum")
  @ApiResponse(
      responseCode = "400",
      description = "Parâmetros de paginação inválidos",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @GetMapping("/sold")
  PageResponse<SoldVehicleResponse> listSold(@ParameterObject @Valid PageParams pageParams) {
    return PageResponse.from(
        listSoldVehicles.execute(pageParams.toPageQuery()),
        SoldVehicleResponse::from
    );
  }
}

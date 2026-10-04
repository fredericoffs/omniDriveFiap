package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.ListVehiclesForSale;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de listagem de veículos.
 */
@Tag(name = "Veículos", description = "Cadastro, consulta, edição, preparação, reentrada e listagens de veículos")
@RestController
@RequestMapping("/vehicles")
public class VehicleListingController {

  private final ListVehiclesForSale listVehiclesForSale;

  VehicleListingController(ListVehiclesForSale listVehiclesForSale) {
    this.listVehiclesForSale = listVehiclesForSale;
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
}

package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.ListVehiclesForSale;
import br.com.fiap.omnidrive.application.usecase.RegisterVehicle;
import br.com.fiap.omnidrive.application.usecase.UpdateVehicle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints de veículos: cadastro, edição e listagem dos veículos à venda.
 */
@Tag(name = "Veículos", description = "Cadastro, edição e listagem de veículos")
@RestController
@RequestMapping("/vehicles")
public class VehicleController {

  // TODO: GET /vehicles/{id} para consultar um veículo (necessário para reler a versão após um 409) e header
  //  Location no 201 do cadastro apontando para ele.
  // TODO: POST /vehicles/{id}/preparation (Recebido → Em Preparação) e POST /vehicles/{id}/release
  //  (Em Preparação → Disponível); sem eles nenhum veículo chega à listagem "à venda" pela API.
  // TODO: POST /vehicles/{id}/readmission (Vendido → Recebido, com novo valor de entrada).
  // TODO: GET /vehicles/sold, ordenado pelo valor da venda (semana 3, junto com Sale).

  private final RegisterVehicle registerVehicle;
  private final UpdateVehicle updateVehicle;
  private final ListVehiclesForSale listVehiclesForSale;

  VehicleController(
      RegisterVehicle registerVehicle,
      UpdateVehicle updateVehicle,
      ListVehiclesForSale listVehiclesForSale
  ) {
    this.registerVehicle = registerVehicle;
    this.updateVehicle = updateVehicle;
    this.listVehiclesForSale = listVehiclesForSale;
  }

  @Operation(
      summary = "Cadastrar veículo",
      description = "Cadastra um veículo que chegou à loja. Ele entra com status RECEIVED (recebido)."
  )
  @ApiResponse(responseCode = "201", description = "Veículo cadastrado")
  @ApiResponse(
      responseCode = "400",
      description = "Corpo da requisição inválido ou campos obrigatórios ausentes",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "Dados que violam uma regra de negócio, como ano fora do intervalo permitido",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  VehicleResponse register(@Valid @RequestBody RegisterVehicleRequest request) {
    return VehicleResponse.from(registerVehicle.execute(request.toInput()));
  }

  @Operation(
      summary = "Editar veículo",
      description = """
          Substitui todos os dados do veículo. Só é permitido com status RECEIVED, IN_PREPARATION ou AVAILABLE. \
          Envie a versão (campo version) que foi lida: se o veículo mudou desde então, a edição é recusada com \
          409 e o veículo deve ser consultado de novo."""
  )
  @ApiResponse(responseCode = "200", description = "Veículo atualizado")
  @ApiResponse(
      responseCode = "400",
      description = "Corpo da requisição inválido ou campos obrigatórios ausentes",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "Veículo não encontrado",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "409",
      description = "O veículo foi alterado por outra operação depois de lido",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "O status atual não permite edição ou os dados violam uma regra de negócio",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PutMapping("/{id}")
  VehicleResponse update(
      @Parameter(description = "Identificador do veículo") @PathVariable UUID id,
      @Valid @RequestBody UpdateVehicleRequest request
  ) {
    return VehicleResponse.from(updateVehicle.execute(id, request.toInput()));
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

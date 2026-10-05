package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.DeleteVehicle;
import br.com.fiap.omnidrive.application.usecase.GetVehicle;
import br.com.fiap.omnidrive.application.usecase.RegisterVehicle;
import br.com.fiap.omnidrive.application.usecase.UpdateVehicle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Endpoints de cadastro de veículos: cadastrar, consultar, editar e excluir. As mudanças de status ficam no
 * {@link VehicleStatusController}, e as listagens no {@link VehicleListingController}.
 */
@Tag(name = "Veículos", description = "Cadastro, consulta, edição, exclusão, preparação, reentrada e listagens de veículos")
@RestController
@RequestMapping("/vehicles")
public class VehicleController {

  private final RegisterVehicle registerVehicle;
  private final GetVehicle getVehicle;
  private final UpdateVehicle updateVehicle;
  private final DeleteVehicle deleteVehicle;

  VehicleController(
      RegisterVehicle registerVehicle,
      GetVehicle getVehicle,
      UpdateVehicle updateVehicle,
      DeleteVehicle deleteVehicle
  ) {
    this.registerVehicle = registerVehicle;
    this.getVehicle = getVehicle;
    this.updateVehicle = updateVehicle;
    this.deleteVehicle = deleteVehicle;
  }

  @Operation(
      summary = "Cadastrar veículo",
      description = "Cadastra um veículo que chegou à loja. Ele entra com status RECEIVED (recebido)."
  )
  @ApiResponse(
      responseCode = "201",
      description = "Veículo cadastrado",
      headers = @Header(name = "Location", description = "URL para consultar o veículo cadastrado")
  )
  @ApiResponse(
      responseCode = "400",
      description = "Corpo da requisição inválido ou campos obrigatórios ausentes",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "409",
      description = "Já existe um veículo com o mesmo chassi ou a mesma placa",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "Dados que violam uma regra de negócio, como ano fora do intervalo permitido, placa fora do "
          + "padrão ou chassi incompatível com o ano",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PostMapping
  ResponseEntity<VehicleResponse> register(@Valid @RequestBody RegisterVehicleRequest request) {
    var vehicle = registerVehicle.execute(request.toInput());
    var location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(vehicle.getId())
        .toUri();
    return ResponseEntity.created(location).body(VehicleResponse.from(vehicle));
  }

  @Operation(
      summary = "Consultar veículo",
      description = """
          Devolve os dados atuais do veículo, inclusive a versão (campo version) que deve ser enviada na edição. \
          Use depois de uma edição recusada com 409 para obter a versão atual."""
  )
  @ApiResponse(responseCode = "200", description = "Veículo encontrado")
  @ApiResponse(
      responseCode = "400",
      description = "Id em formato inválido",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "Veículo não encontrado",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @GetMapping("/{id}")
  VehicleResponse get(@Parameter(description = "Identificador do veículo") @PathVariable UUID id) {
    return VehicleResponse.from(getVehicle.execute(id));
  }

  @Operation(
      summary = "Editar veículo",
      description = """
          Substitui todos os dados do veículo, exceto o chassi, que não é editável. Só é permitido com status \
          RECEIVED, IN_PREPARATION ou AVAILABLE. \
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
      description = "O veículo foi alterado por outra operação depois de lido, ou a nova placa pertence a outro "
          + "veículo",
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
      summary = "Excluir veículo",
      description = """
          Exclui um veículo cadastrado por engano, junto com o seu histórico de status. Só é permitido com status \
          RECEIVED e se o veículo nunca teve venda: um veículo que voltou à loja pela reentrada não pode ser \
          excluído."""
  )
  @ApiResponse(responseCode = "204", description = "Veículo excluído")
  @ApiResponse(
      responseCode = "400",
      description = "Id em formato inválido",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "Veículo não encontrado",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "409",
      description = "O veículo foi alterado por outra operação durante a exclusão",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "O veículo não está com status RECEIVED ou já teve venda",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @DeleteMapping("/{id}")
  ResponseEntity<Void> delete(@Parameter(description = "Identificador do veículo") @PathVariable UUID id) {
    deleteVehicle.execute(id);
    return ResponseEntity.noContent().build();
  }
}

package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.GetVehicle;
import br.com.fiap.omnidrive.application.usecase.ListVehiclesForSale;
import br.com.fiap.omnidrive.application.usecase.ReadmitVehicle;
import br.com.fiap.omnidrive.application.usecase.RegisterVehicle;
import br.com.fiap.omnidrive.application.usecase.ReleaseVehicleForSale;
import br.com.fiap.omnidrive.application.usecase.StartVehiclePreparation;
import br.com.fiap.omnidrive.application.usecase.UpdateVehicle;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Endpoints de veículos: cadastro, consulta, edição, preparação, liberação para venda, reentrada e listagem dos veículos à venda.
 */
@Tag(name = "Veículos", description = "Cadastro, consulta, edição, preparação, reentrada e listagem de veículos")
@RestController
@RequestMapping("/vehicles")
public class VehicleController {

  // TODO: GET /vehicles/sold, ordenado pelo valor da venda (semana 3, junto com Sale).

  private final RegisterVehicle registerVehicle;
  private final GetVehicle getVehicle;
  private final UpdateVehicle updateVehicle;
  private final StartVehiclePreparation startVehiclePreparation;
  private final ReleaseVehicleForSale releaseVehicleForSale;
  private final ReadmitVehicle readmitVehicle;
  private final ListVehiclesForSale listVehiclesForSale;

  VehicleController(
      RegisterVehicle registerVehicle,
      GetVehicle getVehicle,
      UpdateVehicle updateVehicle,
      StartVehiclePreparation startVehiclePreparation,
      ReleaseVehicleForSale releaseVehicleForSale,
      ReadmitVehicle readmitVehicle,
      ListVehiclesForSale listVehiclesForSale
  ) {
    this.registerVehicle = registerVehicle;
    this.getVehicle = getVehicle;
    this.updateVehicle = updateVehicle;
    this.startVehiclePreparation = startVehiclePreparation;
    this.releaseVehicleForSale = releaseVehicleForSale;
    this.readmitVehicle = readmitVehicle;
    this.listVehiclesForSale = listVehiclesForSale;
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
      summary = "Iniciar preparação",
      description = "Coloca em preparação um veículo com status RECEIVED (recebido). Ele passa a IN_PREPARATION."
  )
  @ApiResponse(responseCode = "200", description = "Veículo em preparação")
  @ApiResponse(
      responseCode = "404",
      description = "Veículo não encontrado",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "409",
      description = "O veículo foi alterado por outra operação durante a mudança de status",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "O veículo não está com status RECEIVED",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PostMapping("/{id}/preparation")
  VehicleResponse startPreparation(@Parameter(description = "Identificador do veículo") @PathVariable UUID id) {
    return VehicleResponse.from(startVehiclePreparation.execute(id));
  }

  @Operation(
      summary = "Liberar para venda",
      description = """
          Libera para venda um veículo com status IN_PREPARATION (em preparação). Ele passa a AVAILABLE e entra \
          na listagem de veículos à venda."""
  )
  @ApiResponse(responseCode = "200", description = "Veículo disponível para venda")
  @ApiResponse(
      responseCode = "404",
      description = "Veículo não encontrado",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "409",
      description = "O veículo foi alterado por outra operação durante a mudança de status",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "O veículo não está com status IN_PREPARATION",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PostMapping("/{id}/release")
  VehicleResponse release(@Parameter(description = "Identificador do veículo") @PathVariable UUID id) {
    return VehicleResponse.from(releaseVehicleForSale.execute(id));
  }

  @Operation(
      summary = "Registrar reentrada",
      description = """
          Registra a volta à loja de um veículo com status SOLD (vendido), com um novo valor de entrada e a \
          quilometragem atual, que deve ser maior que a registrada. Ele passa a RECEIVED e segue de novo pela \
          preparação e pela liberação. O valor de entrada e a quilometragem anteriores ficam no histórico."""
  )
  @ApiResponse(responseCode = "200", description = "Veículo recebido de volta")
  @ApiResponse(
      responseCode = "400",
      description = "Corpo da requisição inválido, ou valor de entrada ou quilometragem ausentes",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "Veículo não encontrado",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "409",
      description = "O veículo foi alterado por outra operação durante a mudança de status",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "O veículo não está com status SOLD, ou a quilometragem não é maior que a atual",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PostMapping("/{id}/readmission")
  VehicleResponse readmit(
      @Parameter(description = "Identificador do veículo") @PathVariable UUID id,
      @Valid @RequestBody ReadmitVehicleRequest request
  ) {
    return VehicleResponse.from(readmitVehicle.execute(id, request.toInput()));
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

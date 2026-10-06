package br.com.fiap.omnidrive.infrastructure.web.vehicle;

import br.com.fiap.omnidrive.application.usecase.vehicle.ReadmitVehicle;
import br.com.fiap.omnidrive.application.usecase.vehicle.ReleaseVehicleForSale;
import br.com.fiap.omnidrive.application.usecase.vehicle.ReturnVehicleToOwner;
import br.com.fiap.omnidrive.application.usecase.vehicle.StartVehiclePreparation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints do ciclo de status do veículo: iniciar a preparação, liberar para venda, devolver o consignado ao dono e
 * registrar a reentrada. As mudanças ligadas à venda (reserva, confirmação e estorno) vêm da venda e do webhook de
 * pagamento.
 */
@Tag(name = "Veículos", description = "Cadastro, consulta, edição, exclusão, mudanças de status e listagens de veículos")
@RestController
@RequestMapping("/vehicles")
public class VehicleStatusController {

  private final StartVehiclePreparation startVehiclePreparation;
  private final ReleaseVehicleForSale releaseVehicleForSale;
  private final ReturnVehicleToOwner returnVehicleToOwner;
  private final ReadmitVehicle readmitVehicle;

  VehicleStatusController(
      StartVehiclePreparation startVehiclePreparation,
      ReleaseVehicleForSale releaseVehicleForSale,
      ReturnVehicleToOwner returnVehicleToOwner,
      ReadmitVehicle readmitVehicle
  ) {
    this.startVehiclePreparation = startVehiclePreparation;
    this.releaseVehicleForSale = releaseVehicleForSale;
    this.returnVehicleToOwner = returnVehicleToOwner;
    this.readmitVehicle = readmitVehicle;
  }

  @Operation(
      operationId = "startVehiclePreparation",
      summary = "Iniciar preparação",
      description = """
          Coloca em preparação um veículo com status RECEIVED (recebido), na entrada, ou AVAILABLE (disponível), \
          quando ele sai da vitrine para reparo. Ele passa a IN_PREPARATION e deixa de aparecer na listagem à \
          venda até ser liberado de novo. O motivo fica registrado no histórico de status."""
  )
  @ApiResponse(responseCode = "200", description = "Veículo em preparação")
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
      description = "O veículo foi alterado por outra operação durante a mudança de status",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "O veículo não está com status RECEIVED nem AVAILABLE",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PostMapping("/{id}/preparation")
  VehicleResponse startPreparation(@Parameter(description = "Identificador do veículo") @PathVariable UUID id) {
    return VehicleResponse.from(startVehiclePreparation.execute(id));
  }

  @Operation(
      operationId = "releaseVehicleForSale",
      summary = "Liberar para venda",
      description = """
          Libera para venda um veículo com status IN_PREPARATION (em preparação). Ele passa a AVAILABLE e entra \
          na listagem de veículos à venda."""
  )
  @ApiResponse(responseCode = "200", description = "Veículo disponível para venda")
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
      operationId = "returnVehicleToOwner",
      summary = "Devolver ao dono",
      description = """
          Devolve ao dono um veículo consignado (CONSIGNED) com status RECEIVED (recebido), IN_PREPARATION (em \
          preparação) ou AVAILABLE (disponível), quando ele sai da loja sem ser vendido. Ele passa a \
          RETURNED_TO_OWNER, deixa de aparecer na listagem à venda e não pode mais ser editado. Se voltar à loja, \
          entra pela reentrada."""
  )
  @ApiResponse(responseCode = "200", description = "Veículo devolvido ao dono")
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
      description = "O veículo foi alterado por outra operação durante a mudança de status",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "O veículo não é consignado, ou não está com status RECEIVED, IN_PREPARATION nem AVAILABLE",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PostMapping("/{id}/return-to-owner")
  VehicleResponse returnToOwner(@Parameter(description = "Identificador do veículo") @PathVariable UUID id) {
    return VehicleResponse.from(returnVehicleToOwner.execute(id));
  }

  @Operation(
      operationId = "readmitVehicle",
      summary = "Registrar reentrada",
      description = """
          Registra a volta à loja de um veículo com status SOLD (vendido) ou RETURNED_TO_OWNER (devolvido ao \
          dono), com um novo valor de entrada e a quilometragem atual, que deve ser maior que a registrada. Vindo \
          de RETURNED_TO_OWNER, a quilometragem também pode ser igual. Ele passa a RECEIVED e segue de novo pela \
          preparação e pela liberação. O valor de entrada e a quilometragem anteriores ficam no histórico."""
  )
  @ApiResponse(responseCode = "200", description = "Veículo recebido de volta")
  @ApiResponse(
      responseCode = "400",
      description = "Id em formato inválido, corpo da requisição inválido, ou valor de entrada ou quilometragem "
          + "ausentes",
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
      description = """
          O veículo não está com status SOLD nem RETURNED_TO_OWNER, ou a quilometragem é menor que a atual (ou \
          igual, vindo de SOLD)""",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PostMapping("/{id}/readmission")
  VehicleResponse readmit(
      @Parameter(description = "Identificador do veículo") @PathVariable UUID id,
      @Valid @RequestBody ReadmitVehicleRequest request
  ) {
    return VehicleResponse.from(readmitVehicle.execute(id, request.toInput()));
  }
}

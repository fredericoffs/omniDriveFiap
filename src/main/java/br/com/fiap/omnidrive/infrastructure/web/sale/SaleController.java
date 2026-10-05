package br.com.fiap.omnidrive.infrastructure.web.sale;

import br.com.fiap.omnidrive.application.usecase.CreateSale;
import br.com.fiap.omnidrive.application.usecase.GetSale;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Endpoints de vendas: efetuar a venda de um veículo e consultar a venda com o status do pagamento.
 */
@Tag(name = "Vendas", description = "Venda de veículos e consulta do pagamento")
@RestController
@RequestMapping("/sales")
public class SaleController {

  private final CreateSale createSale;
  private final GetSale getSale;

  SaleController(
      CreateSale createSale,
      GetSale getSale
  ) {
    this.createSale = createSale;
    this.getSale = getSale;
  }

  @Operation(
      summary = "Efetuar venda",
      description = """
          Vende um veículo com status AVAILABLE (disponível) para o comprador informado. O valor da venda é o \
          preço de venda atual do veículo, que passa a RESERVED (reservado) até o processador de pagamento \
          informar o resultado pelo webhook, usando o código do pagamento devolvido aqui."""
  )
  @ApiResponse(
      responseCode = "201",
      description = "Venda efetuada, com o pagamento em AWAITING_PAYMENT (aguardando pagamento)",
      headers = @Header(name = "Location", description = "URL da venda criada")
  )
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
      description = "O veículo foi alterado por outra operação durante a venda, como uma venda simultânea",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "O veículo não está com status AVAILABLE, o CPF é inválido ou a data da venda está no futuro",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PostMapping
  ResponseEntity<SaleResponse> create(@Valid @RequestBody CreateSaleRequest request) {
    var result = createSale.execute(request.toInput());
    var location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(result.sale().getId())
        .toUri();
    return ResponseEntity.created(location).body(SaleResponse.from(result));
  }

  @Operation(
      summary = "Consultar venda",
      description = """
          Devolve a venda com o status atual do pagamento, que muda conforme o processador de pagamento informa \
          o resultado pelo webhook. É a URL devolvida no header Location ao efetuar a venda."""
  )
  @ApiResponse(responseCode = "200", description = "Venda encontrada")
  @ApiResponse(
      responseCode = "400",
      description = "Id em formato inválido",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "404",
      description = "Venda não encontrada",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @GetMapping("/{id}")
  SaleResponse get(@Parameter(description = "Identificador da venda") @PathVariable UUID id) {
    return SaleResponse.from(getSale.execute(id));
  }
}

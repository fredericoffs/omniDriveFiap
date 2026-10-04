package br.com.fiap.omnidrive.infrastructure.web.sale;

import br.com.fiap.omnidrive.application.usecase.CreateSale;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Endpoints de vendas: efetuar a venda de um veículo.
 */
@Tag(name = "Vendas", description = "Venda de veículos")
@RestController
@RequestMapping("/sales")
public class SaleController {

  // TODO: GET /sales/{id}, para consultar a venda e o status do pagamento (o Location do POST já aponta para ele).

  private final CreateSale createSale;

  SaleController(CreateSale createSale) {
    this.createSale = createSale;
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
}

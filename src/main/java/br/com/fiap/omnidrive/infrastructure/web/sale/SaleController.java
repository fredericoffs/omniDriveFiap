package br.com.fiap.omnidrive.infrastructure.web.sale;

import br.com.fiap.omnidrive.application.usecase.sale.CancelSale;
import br.com.fiap.omnidrive.application.usecase.sale.CreateSale;
import br.com.fiap.omnidrive.application.usecase.sale.GetSale;
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
 * Endpoints de vendas: efetuar a venda de um veículo, consultar a venda com o status do pagamento e cancelar a venda
 * enquanto o pagamento aguarda.
 */
@Tag(name = "Vendas", description = "Venda de veículos, consulta do pagamento e cancelamento")
@RestController
@RequestMapping("/sales")
public class SaleController {

  private final CreateSale createSale;
  private final GetSale getSale;
  private final CancelSale cancelSale;

  SaleController(
      CreateSale createSale,
      GetSale getSale,
      CancelSale cancelSale
  ) {
    this.createSale = createSale;
    this.getSale = getSale;
    this.cancelSale = cancelSale;
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

  @Operation(
      summary = "Cancelar venda",
      description = """
          Cancela a venda a pedido da loja, enquanto o pagamento está em AWAITING_PAYMENT (aguardando pagamento). \
          O pagamento passa a CANCELED (cancelado) e o veículo volta a AVAILABLE (disponível). Premissa: a loja \
          também cancela a cobrança no processador de pagamento; um PAID que chegue depois pelo webhook é \
          recusado com 422. Cancelar de novo uma venda já cancelada responde 200 sem mudar nada."""
  )
  @ApiResponse(responseCode = "200", description = "Venda cancelada, com o pagamento em CANCELED")
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
  @ApiResponse(
      responseCode = "409",
      description = "O pagamento ou o veículo foram alterados por outra operação durante o cancelamento, como um "
          + "webhook simultâneo",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @ApiResponse(
      responseCode = "422",
      description = "O pagamento já tem resultado: PAID, DECLINED ou REFUNDED",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class))
  )
  @PostMapping("/{id}/cancellation")
  SaleResponse cancel(@Parameter(description = "Identificador da venda") @PathVariable UUID id) {
    return SaleResponse.from(cancelSale.execute(id));
  }
}

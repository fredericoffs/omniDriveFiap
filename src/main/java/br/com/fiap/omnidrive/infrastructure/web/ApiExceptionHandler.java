package br.com.fiap.omnidrive.infrastructure.web;

import br.com.fiap.omnidrive.domain.exception.AlreadyExistsException;
import br.com.fiap.omnidrive.domain.exception.BusinessRuleException;
import br.com.fiap.omnidrive.domain.exception.ConcurrentUpdateException;
import br.com.fiap.omnidrive.domain.exception.NotFoundException;
import br.com.fiap.omnidrive.infrastructure.web.webhook.InvalidWebhookSecretException;
import java.util.LinkedHashMap;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Converte as exceções em respostas no formato Problem Details (RFC 9457), com mensagens em português.
 *
 * <ul>
 *     <li>{@link NotFoundException} → 404</li>
 *     <li>{@link ConcurrentUpdateException} e {@link AlreadyExistsException} → 409</li>
 *     <li>{@link BusinessRuleException} → 422</li>
 *     <li>{@link InvalidWebhookSecretException} → 401</li>
 *     <li>Validação do corpo ou dos parâmetros de paginação, JSON inválido ou parâmetro com formato errado → 400</li>
 * </ul>
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String INVALID_REQUEST = "Requisição inválida";
  private static final String INVALID_VALUE = "Valor inválido";

  private static ProblemDetail problem(
      HttpStatus status,
      String title,
      String detail
  ) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setTitle(title);
    return problem;
  }

  @ExceptionHandler(NotFoundException.class)
  ProblemDetail handleNotFound(NotFoundException exception) {
    return problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", exception.getMessage());
  }

  @ExceptionHandler(ConcurrentUpdateException.class)
  ProblemDetail handleConcurrentUpdate(ConcurrentUpdateException exception) {
    return problem(HttpStatus.CONFLICT, "Conflito de atualização", exception.getMessage());
  }

  @ExceptionHandler(AlreadyExistsException.class)
  ProblemDetail handleAlreadyExists(AlreadyExistsException exception) {
    return problem(HttpStatus.CONFLICT, "Registro duplicado", exception.getMessage());
  }

  @ExceptionHandler(BusinessRuleException.class)
  ProblemDetail handleBusinessRule(BusinessRuleException exception) {
    return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Regra de negócio violada", exception.getMessage());
  }

  @ExceptionHandler(InvalidWebhookSecretException.class)
  ProblemDetail handleInvalidWebhookSecret(InvalidWebhookSecretException exception) {
    return problem(HttpStatus.UNAUTHORIZED, "Não autorizado", exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
    return problem(
        HttpStatus.BAD_REQUEST,
        INVALID_REQUEST,
        "Valor inválido para o parâmetro " + exception.getName()
    );
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request
  ) {
    var errors = new LinkedHashMap<String, String>();
    exception.getBindingResult().getFieldErrors()
        .forEach(error -> errors.putIfAbsent(
            error.getField(),
            error.isBindingFailure() ? INVALID_VALUE : error.getDefaultMessage()
        ));
    var problem = problem(HttpStatus.BAD_REQUEST, INVALID_REQUEST, "Um ou mais campos são inválidos");
    problem.setProperty("errors", errors);
    return ResponseEntity.badRequest().body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      @NonNull HttpMessageNotReadableException exception,
      @NonNull HttpHeaders headers,
      @NonNull HttpStatusCode status,
      @NonNull WebRequest request
  ) {
    var problem = problem(
        HttpStatus.BAD_REQUEST,
        INVALID_REQUEST,
        "O corpo da requisição não pôde ser lido. Verifique o JSON e os valores dos campos"
    );
    return ResponseEntity.badRequest().body(problem);
  }
}

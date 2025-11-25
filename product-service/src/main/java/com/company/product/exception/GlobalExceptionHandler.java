package com.company.product.exception;

import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ProblemDetail buildProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            String code,
            ServerHttpRequest request
    ) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setProperty("codigo", code);
        pd.setProperty("ruta", request.getPath().value());
        pd.setProperty("timestamp", OffsetDateTime.now());
        return pd;
    }

    @ExceptionHandler(NotFoundException.class)
    public Mono<@NonNull ProblemDetail> handleNotFound(
            NotFoundException ex,
            ServerHttpRequest request
    ) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        ProblemDetail pd = buildProblemDetail(
                HttpStatus.NOT_FOUND,
                "RECURSO_NO_ENCONTRADO",
                ex.getMessage(),
                "PRODUCTO_NO_ENCONTRADO",
                request
        );
        return Mono.just(pd);
    }

    @ExceptionHandler(BadRequestException.class)
    public Mono<@NonNull ProblemDetail> handleBadRequest(
            BadRequestException ex,
            ServerHttpRequest request
    ) {
        log.warn("Solicitud inválida: {}", ex.getMessage());
        ProblemDetail pd = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "SOLICITUD_INVALIDA",
                ex.getMessage(),
                "BAD_REQUEST",
                request
        );
        return Mono.just(pd);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<@NonNull ProblemDetail> handleWebExchangeBind(
            WebExchangeBindException ex,
            ServerHttpRequest request
    ) {
        log.warn("Errores de validación en el cuerpo de la petición: {}", ex.getMessage());

        List<Map<String, String>> errors = ex.getFieldErrors().stream()
                .map(err -> Map.of(
                        "campo", err.getField(),
                        "mensaje", err.getDefaultMessage() != null
                                ? err.getDefaultMessage()
                                : "Valor inválido."
                ))
                .toList();

        ProblemDetail pd = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "SOLICITUD_INVALIDA",
                "La solicitud contiene errores de validación. Corrija los campos indicados y vuelva a intentarlo.",
                "VALIDACION",
                request
        );
        pd.setProperty("errores", errors);
        return Mono.just(pd);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Mono<@NonNull ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex,
            ServerHttpRequest request
    ) {
        log.warn("Errores de validación en parámetros de la petición: {}", ex.getMessage());

        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(cv -> Map.of(
                        "campo", cv.getPropertyPath().toString(),
                        "mensaje", cv.getMessage()
                ))
                .toList();

        ProblemDetail pd = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "SOLICITUD_INVALIDA",
                "Uno o más parámetros de la solicitud no son válidos.",
                "VALIDACION_PARAMETROS",
                request
        );
        pd.setProperty("errores", errors);
        return Mono.just(pd);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<@NonNull ProblemDetail> handleServerWebInput(
            ServerWebInputException ex,
            ServerHttpRequest request
    ) {
        log.warn("Entrada inválida: {}", ex.getMessage());

        ProblemDetail pd = buildProblemDetail(
                HttpStatus.BAD_REQUEST,
                "ENTRADA_INVALIDA",
                "El formato de la solicitud es inválido o faltan datos obligatorios. " +
                        "Verifique el cuerpo JSON, los parámetros y el tipo de los campos.",
                "ENTRADA_INVALIDA",
                request
        );
        return Mono.just(pd);
    }

    @ExceptionHandler(DataAccessException.class)
    public Mono<@NonNull ProblemDetail> handleDataAccess(
            DataAccessException ex,
            ServerHttpRequest request
    ) {
        log.error("Error de acceso a datos en Product Service", ex);

        ProblemDetail pd = buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERROR_DE_DATOS",
                "Ocurrió un error al acceder a la base de datos. Intente nuevamente más tarde.",
                "ERROR_DE_DATOS",
                request
        );
        return Mono.just(pd);
    }

    @ExceptionHandler(Exception.class)
    public Mono<@NonNull ProblemDetail> handleGeneric(
            Exception ex,
            ServerHttpRequest request
    ) {
        log.error("Error inesperado en Product Service", ex);

        ProblemDetail pd = buildProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ERROR_INTERNO",
                "Ocurrió un error interno inesperado. Intente nuevamente más tarde.",
                "ERROR_INTERNO",
                request
        );
        return Mono.just(pd);
    }
}

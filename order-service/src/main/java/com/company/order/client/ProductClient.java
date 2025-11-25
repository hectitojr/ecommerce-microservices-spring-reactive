package com.company.order.client;

import com.company.order.exception.BadRequestException;
import com.company.order.exception.BusinessException;
import com.company.order.exception.ExternalServiceException;
import com.company.order.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductClient {

    private final WebClient productWebClient;

    public Mono<Boolean> checkAvailability(Long productId, Integer qty) {
        String action = "verificar la disponibilidad del producto";

        return productWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/products/{id}/availability")
                        .queryParam("qty", qty)
                        .build(productId)
                )
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> handle4xx(response, action))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> handle5xx(response, action))
                .bodyToMono(Boolean.class)
                .doOnSubscribe(s -> log.debug(
                        "Llamando a ProductService [availability]: accion={}, productId={}, qty={}",
                        action, productId, qty
                ))
                .onErrorMap(ex -> mapException(ex, action));
    }

    public Mono<RemoteProduct> decreaseStock(Long productId, Integer qty) {
        String action = "descontar el stock del producto";

        return productWebClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/products/{id}/stock/decrease")
                        .queryParam("qty", qty)
                        .build(productId)
                )
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError,
                        response -> handle4xx(response, action))
                .onStatus(HttpStatusCode::is5xxServerError,
                        response -> handle5xx(response, action))
                .bodyToMono(RemoteProduct.class)
                .doOnSubscribe(s -> log.debug(
                        "Llamando a ProductService [decreaseStock]: accion={}, productId={}, qty={}",
                        action, productId, qty
                ))
                .onErrorMap(ex -> mapException(ex, action));
    }

    private Mono<? extends Throwable> handle4xx(ClientResponse response, String action) {
        HttpStatusCode status = response.statusCode();
        return response.bodyToMono(ProblemDetail.class)
                .defaultIfEmpty(ProblemDetail.forStatus(HttpStatus.BAD_REQUEST))
                .map(pd -> {
                    String detail = pd.getDetail();
                    if (detail == null || detail.isBlank()) {
                        detail = "Error en la solicitud enviada al servicio de productos.";
                    }

                    log.warn("Error 4xx desde ProductService al {}: status={}, detail={}",
                            action, status, detail);

                    if (status.equals(HttpStatus.NOT_FOUND)) {
                        return new NotFoundException(
                                "El producto no existe o no está disponible."
                        );
                    }

                    if (status.equals(HttpStatus.BAD_REQUEST)) {
                        return new BusinessException(
                                "Error de negocio al " + action + ": " + detail
                        );
                    }

                    return new BadRequestException(
                            "Error en la solicitud al " + action + ": " + detail
                    );
                });
    }

    private Mono<? extends Throwable> handle5xx(ClientResponse response, String action) {
        HttpStatusCode status = response.statusCode();
        return response.bodyToMono(ProblemDetail.class)
                .defaultIfEmpty(ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR))
                .map(pd -> {
                    String detail = pd.getDetail();
                    log.error("Error 5xx desde ProductService al {}: status={}, detail={}",
                            action, status, detail);
                    return new ExternalServiceException(
                            "El servicio de productos presentó un error al " + action +
                                    ". Intente nuevamente más tarde.",
                            null
                    );
                });
    }

    private Throwable mapException(Throwable ex, String action) {

        if (ex instanceof NotFoundException
                || ex instanceof BusinessException
                || ex instanceof BadRequestException
                || ex instanceof ExternalServiceException) {
            return ex;
        }

        if (ex instanceof WebClientRequestException reqEx) {
            log.error("Error de red al {} en ProductService", action, reqEx);
            return new ExternalServiceException(
                    "Error de red al " + action + " en el servicio de productos.",
                    reqEx
            );
        }

        log.error("Error inesperado al {} en ProductService", action, ex);
        return new ExternalServiceException(
                "Error inesperado al " + action + " en el servicio de productos.",
                ex
        );
    }
}

package com.company.product.controller;

import com.company.product.dto.CreateProductRequest;
import com.company.product.dto.UpdateProductRequest;
import com.company.product.exception.BadRequestException;
import com.company.product.exception.GlobalExceptionHandler;
import com.company.product.exception.NotFoundException;
import com.company.product.model.Product;
import com.company.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ProductService service;

    @Test
    void create_shouldReturnCreatedProduct() {
        CreateProductRequest req = new CreateProductRequest(
                "Laptop", 1500.0, 10
        );
        Product product = new Product(1L, "Laptop", 1500.0, 10, true);

        when(service.create(any(CreateProductRequest.class)))
                .thenReturn(Mono.just(product));

        webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Laptop")
                .jsonPath("$.price").isEqualTo(1500.0)
                .jsonPath("$.stock").isEqualTo(10)
                .jsonPath("$.active").isEqualTo(true);
    }

    @Test
    void create_withInvalidBody_shouldReturnValidationErrors() {
        CreateProductRequest invalid = new CreateProductRequest(
                "", -10.0, -1
        );

        webTestClient.post()
                .uri("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalid)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("SOLICITUD_INVALIDA")
                .jsonPath("$.codigo").isEqualTo("VALIDACION")
                .jsonPath("$.errores").isArray();
    }

    @Test
    void listActive_shouldReturnFluxOfProducts() {
        Product p1 = new Product(1L, "Mouse", 50.0, 100, true);
        Product p2 = new Product(2L, "Teclado", 80.0, 50, true);

        when(service.listActive()).thenReturn(Flux.just(p1, p2));

        webTestClient.get()
                .uri("/products")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].id").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Mouse")
                .jsonPath("$[1].id").isEqualTo(2)
                .jsonPath("$[1].name").isEqualTo("Teclado");
    }

    @Test
    void findById_whenExists_shouldReturnProduct() {
        Long id = 1L;
        Product product = new Product(id, "Monitor", 500.0, 20, true);

        when(service.findById(id)).thenReturn(Mono.just(product));

        webTestClient.get()
                .uri("/products/{id}", id)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Monitor");
    }

    @Test
    void findById_whenNotFound_shouldReturn404WithProblemDetail() {
        Long id = 99L;

        when(service.findById(id))
                .thenReturn(Mono.error(new NotFoundException("El producto solicitado no existe.")));

        webTestClient.get()
                .uri("/products/{id}", id)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.title").isEqualTo("RECURSO_NO_ENCONTRADO")
                .jsonPath("$.codigo").isEqualTo("PRODUCTO_NO_ENCONTRADO");
    }

    @Test
    void update_shouldReturnUpdatedProduct() {
        Long id = 1L;
        UpdateProductRequest req = new UpdateProductRequest(
                "Nuevo Nombre", 120.0, 15, true
        );
        Product updated = new Product(id, "Nuevo Nombre", 120.0, 15, true);

        when(service.update(eq(id), any(UpdateProductRequest.class)))
                .thenReturn(Mono.just(updated));

        webTestClient.put()
                .uri("/products/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Nuevo Nombre")
                .jsonPath("$.price").isEqualTo(120.0)
                .jsonPath("$.stock").isEqualTo(15)
                .jsonPath("$.active").isEqualTo(true);
    }

    @Test
    void availability_shouldReturnTrueOrFalse() {
        Long id = 1L;
        Integer qty = 5;

        when(service.checkAvailability(id, qty))
                .thenReturn(Mono.just(Boolean.TRUE));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/products/{id}/availability")
                        .queryParam("qty", qty)
                        .build(id)
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody(Boolean.class)
                .isEqualTo(Boolean.TRUE);
    }

    @Test
    void availability_withInvalidQty_shouldReturnBadRequest() {
        Long id = 1L;
        Integer invalidQty = 0;

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/products/{id}/availability")
                        .queryParam("qty", invalidQty)
                        .build(id)
                )
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("SOLICITUD_INVALIDA")
                .jsonPath("$.codigo").isEqualTo("VALIDACION_PARAMETROS");
    }

    @Test
    void decreaseStock_shouldReturnUpdatedProduct() {
        Long id = 1L;
        Integer qty = 2;
        Product updated = new Product(id, "Laptop", 1500.0, 8, true);

        when(service.decreaseStock(id, qty))
                .thenReturn(Mono.just(updated));

        webTestClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/products/{id}/stock/decrease")
                        .queryParam("qty", qty)
                        .build(id)
                )
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(1)
                .jsonPath("$.stock").isEqualTo(8);
    }

    @Test
    void decreaseStock_whenServiceThrowsBadRequest_shouldReturn400WithProblemDetail() {
        Long id = 1L;
        Integer qty = 100;

        when(service.decreaseStock(id, qty))
                .thenReturn(Mono.error(new BadRequestException("No hay stock suficiente para completar la operación.")));

        webTestClient.patch()
                .uri(uriBuilder -> uriBuilder
                        .path("/products/{id}/stock/decrease")
                        .queryParam("qty", qty)
                        .build(id)
                )
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.title").isEqualTo("SOLICITUD_INVALIDA")
                .jsonPath("$.codigo").isEqualTo("BAD_REQUEST")
                .jsonPath("$.detail").isEqualTo("No hay stock suficiente para completar la operación.");
    }
}

package com.company.product.controller;

import com.company.product.dto.CreateProductRequest;
import com.company.product.dto.ProductAvailabilityResponse;
import com.company.product.dto.UpdateProductRequest;
import com.company.product.model.Product;
import com.company.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Product> create(@Valid @RequestBody CreateProductRequest req) {
        return service.create(req);
    }

    @GetMapping
    public Flux<Product> listActive() {
        return service.listActive();
    }

    @GetMapping("/{id}")
    public Mono<Product> findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PutMapping("/{id}")
    public Mono<Product> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest req
    ) {
        return service.update(id, req);
    }

    @GetMapping("/{id}/availability")
    public Mono<Boolean> availability(
            @PathVariable Long id,
            @RequestParam
            @Min(value = 1, message = "La cantidad mínima solicitada debe ser 1.")
            Integer qty
    ) {
        return service.checkAvailability(id, qty);
    }

    @GetMapping("/{id}/availability/details")
    public Mono<ProductAvailabilityResponse> availabilityDetails(
            @PathVariable Long id,
            @RequestParam
            @Min(value = 1, message = "La cantidad mínima solicitada debe ser 1.")
            Integer qty
    ) {
        return service.checkAvailabilityDetails(id, qty);
    }

    @PatchMapping("/{id}/stock/decrease")
    public Mono<Product> decreaseStock(
            @PathVariable Long id,
            @RequestParam
            @Min(value = 1, message = "La cantidad mínima a descontar debe ser 1.")
            Integer qty
    ) {
        return service.decreaseStock(id, qty);
    }
}

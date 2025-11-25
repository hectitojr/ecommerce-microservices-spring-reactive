package com.company.product.service;

import com.company.product.dto.CreateProductRequest;
import com.company.product.dto.UpdateProductRequest;
import com.company.product.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductService {

    Mono<Product> create(CreateProductRequest req);

    Mono<Product> findById(Long id);

    Flux<Product> listActive();

    Mono<Product> update(Long id, UpdateProductRequest req);

    Mono<Boolean> checkAvailability(Long id, Integer qty);

    Mono<Product> decreaseStock(Long id, Integer qty);
}

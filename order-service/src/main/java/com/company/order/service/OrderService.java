package com.company.order.service;

import com.company.order.dto.CreateOrderRequest;
import com.company.order.model.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OrderService {

    Mono<Order> create(CreateOrderRequest req);

    Mono<Order> findById(Long id);

    Flux<Order> list();
}

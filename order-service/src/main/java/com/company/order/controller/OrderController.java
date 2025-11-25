package com.company.order.controller;

import com.company.order.dto.CreateOrderRequest;
import com.company.order.model.Order;
import com.company.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Order> create(@Valid @RequestBody CreateOrderRequest req) {
        return service.create(req);
    }

    @GetMapping("/{id}")
    public Mono<Order> findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public Flux<Order> list() {
        return service.list();
    }
}

package com.company.order.service;

import com.company.order.client.ProductClient;
import com.company.order.dto.CreateOrderRequest;
import com.company.order.exception.BadRequestException;
import com.company.order.exception.NotFoundException;
import com.company.order.model.Order;
import com.company.order.model.OrderStatus;
import com.company.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository repo;
    private final ProductClient productClient;

    private final Predicate<Integer> validQuantity = q -> q != null && q > 0;

    @Override
    public Mono<Order> create(CreateOrderRequest req) {

        Integer qty = Optional.ofNullable(req.quantity())
                .filter(validQuantity)
                .orElseThrow(() ->
                        new BadRequestException("La cantidad debe ser mayor a 0.")
                );

        Long productId = Optional.ofNullable(req.productId())
                .filter(id -> id > 0)
                .orElseThrow(() ->
                        new BadRequestException("El id de producto debe ser mayor a 0.")
                );

        log.info("Creando orden para productId={}, quantity={}", productId, qty);

        return productClient.checkAvailability(productId, qty)
                .flatMap(available -> {
                    if (!available) {
                        log.warn("Stock insuficiente para productId={}, requestedQty={}", productId, qty);
                        Order rejected = new Order(
                                null,
                                productId,
                                qty,
                                0.0,
                                OrderStatus.REJECTED
                        );
                        return repo.save(rejected);
                    }

                    return productClient.decreaseStock(productId, qty)
                            .flatMap(remoteProduct -> {
                                double total = remoteProduct.price() * qty;
                                Order order = new Order(
                                        null,
                                        productId,
                                        qty,
                                        total,
                                        OrderStatus.CREATED
                                );
                                return repo.save(order);
                            });
                })
                .doOnSuccess(order ->
                        log.info("Orden persistida id={}, status={}, productId={}, quantity={}, total={}",
                                order.id(), order.status(), order.productId(),
                                order.quantity(), order.total())
                );
    }

    @Override
    public Mono<Order> findById(Long id) {
        return Mono.justOrEmpty(id)
                .filter(v -> v > 0)
                .switchIfEmpty(Mono.error(
                        new BadRequestException("El id de la orden debe ser mayor a 0.")
                ))
                .flatMap(repo::findById)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("La orden solicitada no existe.")
                ))
                .doOnSubscribe(s -> log.debug("Buscando orden por id={}", id));
    }

    @Override
    public Flux<Order> list() {
        log.debug("Listando todas las órdenes");
        return repo.findAll()
                .sort((o1, o2) -> o1.id().compareTo(o2.id()));
    }
}

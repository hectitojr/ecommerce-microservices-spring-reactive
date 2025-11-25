package com.company.product.service;

import com.company.product.dto.CreateProductRequest;
import com.company.product.dto.UpdateProductRequest;
import com.company.product.exception.BadRequestException;
import com.company.product.exception.NotFoundException;
import com.company.product.model.Product;
import com.company.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repo;

    private final Predicate<Product> isActive = Product::active;

    private final Supplier<BadRequestException> insufficientStock =
            () -> new BadRequestException("No hay stock suficiente para completar la operación.");

    @Override
    public Mono<Product> create(CreateProductRequest req) {
        log.info("Creando producto: name={}, price={}, stock={}",
                req.name(), req.price(), req.stock());

        return Mono.just(req)
                .map(r -> new Product(null, r.name(), r.price(), r.stock(), true))
                .flatMap(repo::save)
                .doOnSuccess(p -> log.info("Producto creado con id={}", p.id()));
    }

    @Override
    public Mono<Product> findById(Long id) {
        return Mono.justOrEmpty(id)
                .filter(i -> i > 0)
                .switchIfEmpty(Mono.error(
                        new BadRequestException("El id de producto debe ser mayor a 0.")
                ))
                .flatMap(repo::findById)
                .switchIfEmpty(Mono.error(
                        new NotFoundException("El producto solicitado no existe.")
                ))
                .doOnSubscribe(s -> log.debug("Buscando producto por id={}", id));
    }

    @Override
    public Flux<Product> listActive() {
        log.debug("Listando productos activos");
        return repo.findByActiveTrue()
                .filter(isActive)
                .sort(Comparator.comparing(Product::name, String.CASE_INSENSITIVE_ORDER));
    }

    @Override
    public Mono<Product> update(Long id, UpdateProductRequest req) {
        log.info("Actualizando producto id={} con: name={}, price={}, stock={}, active={}",
                id, req.name(), req.price(), req.stock(), req.active());

        return findById(id)
                .map(p -> new Product(
                        p.id(),
                        req.name(),
                        req.price(),
                        req.stock(),
                        req.active()
                ))
                .flatMap(repo::save)
                .doOnSuccess(p -> log.info("Producto actualizado id={}", p.id()));
    }

    @Override
    public Mono<Boolean> checkAvailability(Long id, Integer qty) {
        return Mono.justOrEmpty(qty)
                .filter(q -> q > 0)
                .switchIfEmpty(Mono.error(
                        new BadRequestException("La cantidad solicitada debe ser mayor a 0.")
                ))
                .zipWith(findById(id))
                .map(tuple -> {
                    Integer q = tuple.getT1();
                    Product p = tuple.getT2();
                    boolean available = p.stock() >= q && Boolean.TRUE.equals(p.active());
                    log.debug("Verificando disponibilidad producto id={}, requestedQty={}, stock={}, active={}, available={}",
                            p.id(), q, p.stock(), p.active(), available);
                    return available;
                });
    }

    @Override
    public Mono<Product> decreaseStock(Long id, Integer qty) {
        return Mono.justOrEmpty(qty)
                .filter(q -> q > 0)
                .switchIfEmpty(Mono.error(
                        new BadRequestException("La cantidad a descontar debe ser mayor a 0.")
                ))
                .zipWith(findById(id))
                .flatMap(tuple -> {
                    Integer q = tuple.getT1();
                    Product p = tuple.getT2();

                    if (!Boolean.TRUE.equals(p.active())) {
                        log.warn("Intento de descontar stock de producto inactivo id={}", p.id());
                        return Mono.error(new BadRequestException(
                                "El producto no está disponible para la venta."
                        ));
                    }

                    if (p.stock() < q) {
                        log.warn("Stock insuficiente para producto id={}, stock={}, requestedQty={}",
                                p.id(), p.stock(), q);
                        return Mono.error(insufficientStock.get());
                    }

                    int newStock = p.stock() - q;
                    boolean newActive = newStock > 0;

                    Product updated = new Product(
                            p.id(),
                            p.name(),
                            p.price(),
                            newStock,
                            newActive
                    );

                    log.info("Descontando stock producto id={}, oldStock={}, newStock={}, newActive={}",
                            p.id(), p.stock(), newStock, newActive);

                    return repo.save(updated);
                });
    }
}

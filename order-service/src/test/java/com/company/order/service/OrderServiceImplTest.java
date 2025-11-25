package com.company.order.service;

import com.company.order.client.ProductClient;
import com.company.order.client.RemoteProduct;
import com.company.order.dto.CreateOrderRequest;
import com.company.order.exception.BadRequestException;
import com.company.order.exception.NotFoundException;
import com.company.order.model.Order;
import com.company.order.model.OrderStatus;
import com.company.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository repo;

    @Mock
    private ProductClient productClient;

    @InjectMocks
    private OrderServiceImpl service;

    @Test
    void create_withInvalidQuantity_shouldThrowBadRequest() {
        CreateOrderRequest req = new CreateOrderRequest(1L, 0);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.create(req)
        );

        assertThat(ex.getMessage()).isEqualTo("La cantidad debe ser mayor a 0.");
    }

    @Test
    void create_withInvalidProductId_shouldThrowBadRequest() {
        CreateOrderRequest req = new CreateOrderRequest(0L, 1);

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> service.create(req)
        );

        assertThat(ex.getMessage()).isEqualTo("El id de producto debe ser mayor a 0.");
    }

    @Test
    void create_whenAvailabilityFalse_shouldCreateRejectedOrderWithZeroTotal() {
        Long productId = 1L;
        int qty = 5;
        CreateOrderRequest req = new CreateOrderRequest(productId, qty);

        when(productClient.checkAvailability(productId, qty))
                .thenReturn(Mono.just(false));

        when(repo.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order o = invocation.getArgument(0);
                    return Mono.just(new Order(10L, o.productId(), o.quantity(), o.total(), o.status()));
                });

        StepVerifier.create(service.create(req))
                .assertNext(order -> {
                    assertThat(order.id()).isEqualTo(10L);
                    assertThat(order.productId()).isEqualTo(productId);
                    assertThat(order.quantity()).isEqualTo(qty);
                    assertThat(order.total()).isEqualTo(0.0);
                    assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
                })
                .verifyComplete();
    }

    @Test
    void create_whenAvailabilityTrue_shouldCallDecreaseStockAndCreateOrderCreated() {
        Long productId = 1L;
        int qty = 2;
        CreateOrderRequest req = new CreateOrderRequest(productId, qty);

        RemoteProduct remoteProduct = new RemoteProduct(
                productId, "Laptop", 1500.0, 8, true
        );

        when(productClient.checkAvailability(productId, qty))
                .thenReturn(Mono.just(true));

        when(productClient.decreaseStock(productId, qty))
                .thenReturn(Mono.just(remoteProduct));

        when(repo.save(any(Order.class)))
                .thenAnswer(invocation -> {
                    Order o = invocation.getArgument(0);
                    return Mono.just(new Order(20L, o.productId(), o.quantity(), o.total(), o.status()));
                });

        StepVerifier.create(service.create(req))
                .assertNext(order -> {
                    assertThat(order.id()).isEqualTo(20L);
                    assertThat(order.productId()).isEqualTo(productId);
                    assertThat(order.quantity()).isEqualTo(qty);
                    assertThat(order.total()).isEqualTo(1500.0 * qty);
                    assertThat(order.status()).isEqualTo(OrderStatus.CREATED);
                })
                .verifyComplete();
    }

    @Test
    void findById_withInvalidId_shouldThrowBadRequest() {
        Long invalidId = 0L;

        StepVerifier.create(service.findById(invalidId))
                .verifyErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(BadRequestException.class);
                    assertThat(ex.getMessage()).isEqualTo("El id de la orden debe ser mayor a 0.");
                });
    }

    @Test
    void findById_whenNotFound_shouldThrowNotFound() {
        Long id = 99L;

        when(repo.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.findById(id))
                .verifyErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(NotFoundException.class);
                    assertThat(ex.getMessage()).isEqualTo("La orden solicitada no existe.");
                });
    }

    @Test
    void findById_whenExists_shouldReturnOrder() {
        Long id = 1L;
        Order order = new Order(id, 10L, 2, 200.0, OrderStatus.CREATED);

        when(repo.findById(id)).thenReturn(Mono.just(order));

        StepVerifier.create(service.findById(id))
                .assertNext(o -> {
                    assertThat(o.id()).isEqualTo(id);
                    assertThat(o.productId()).isEqualTo(10L);
                    assertThat(o.quantity()).isEqualTo(2);
                    assertThat(o.total()).isEqualTo(200.0);
                    assertThat(o.status()).isEqualTo(OrderStatus.CREATED);
                })
                .verifyComplete();
    }

    @Test
    void list_shouldReturnAllOrdersSortedById() {
        Order o2 = new Order(2L, 10L, 1, 100.0, OrderStatus.CREATED);
        Order o1 = new Order(1L, 11L, 3, 300.0, OrderStatus.REJECTED);

        when(repo.findAll()).thenReturn(Flux.just(o2, o1));

        StepVerifier.create(service.list().collectList())
                .assertNext(list -> {
                    assertThat(list).hasSize(2);
                    assertThat(list.stream().map(Order::id).toList())
                            .containsExactly(1L, 2L);
                })
                .verifyComplete();
    }
}

package com.company.product.service;

import com.company.product.dto.CreateProductRequest;
import com.company.product.dto.UpdateProductRequest;
import com.company.product.exception.BadRequestException;
import com.company.product.exception.NotFoundException;
import com.company.product.model.Product;
import com.company.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository repo;

    @InjectMocks
    private ProductServiceImpl service;

    @Test
    void create_shouldPersistProductWithActiveTrue() {
        CreateProductRequest req = new CreateProductRequest(
                "Laptop", 1500.0, 10
        );

        when(repo.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    return Mono.just(new Product(1L, p.name(), p.price(), p.stock(), p.active()));
                });

        StepVerifier.create(service.create(req))
                .assertNext(p -> {
                    assertThat(p.id()).isEqualTo(1L);
                    assertThat(p.name()).isEqualTo("Laptop");
                    assertThat(p.price()).isEqualTo(1500.0);
                    assertThat(p.stock()).isEqualTo(10);
                    assertThat(p.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void findById_withInvalidId_shouldErrorWithBadRequest() {
        Long invalidId = 0L;

        StepVerifier.create(service.findById(invalidId))
                .verifyErrorSatisfies(ex -> {
                    assertThat(ex)
                            .isInstanceOf(BadRequestException.class)
                            .hasMessage("El id de producto debe ser mayor a 0.");
                });
    }

    @Test
    void findById_whenProductNotFound_shouldErrorWithNotFound() {
        Long id = 10L;

        when(repo.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.findById(id))
                .verifyErrorSatisfies(ex -> {
                    assertThat(ex)
                            .isInstanceOf(NotFoundException.class)
                            .hasMessage("El producto solicitado no existe.");
                });
    }

    @Test
    void findById_whenProductExists_shouldReturnProduct() {
        Long id = 1L;
        Product product = new Product(id, "Mouse", 50.0, 100, true);

        when(repo.findById(id)).thenReturn(Mono.just(product));

        StepVerifier.create(service.findById(id))
                .assertNext(p -> {
                    assertThat(p.id()).isEqualTo(id);
                    assertThat(p.name()).isEqualTo("Mouse");
                    assertThat(p.price()).isEqualTo(50.0);
                    assertThat(p.stock()).isEqualTo(100);
                    assertThat(p.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void listActive_shouldFilterInactiveAndSortByNameIgnoringCase() {
        Product p1 = new Product(1L, "Zapatillas", 100.0, 5, true);
        Product p2 = new Product(2L, "auriculares", 50.0, 10, true);
        Product p3 = new Product(3L, "Mouse", 30.0, 0, false); // debería ser filtrado

        when(repo.findByActiveTrue())
                .thenReturn(Flux.just(p1, p2, p3));

        List<Product> result = new ArrayList<>();

        StepVerifier.create(service.listActive())
                .recordWith(() -> result)
                .expectNextCount(2)
                .verifyComplete();

        assertThat(result)
                .extracting(Product::name)
                .containsExactly("auriculares", "Zapatillas"); // orden alfabético case-insensitive
    }

    @Test
    void update_shouldUpdateExistingProduct() {
        Long id = 1L;
        Product existing = new Product(id, "Teclado", 80.0, 20, true);
        UpdateProductRequest req = new UpdateProductRequest(
                "Teclado Mecánico", 120.0, 15, true
        );

        when(repo.findById(id)).thenReturn(Mono.just(existing));
        when(repo.save(any(Product.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.update(id, req))
                .assertNext(p -> {
                    assertThat(p.id()).isEqualTo(id);
                    assertThat(p.name()).isEqualTo("Teclado Mecánico");
                    assertThat(p.price()).isEqualTo(120.0);
                    assertThat(p.stock()).isEqualTo(15);
                    assertThat(p.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void checkAvailability_withInvalidQty_shouldErrorWithBadRequest() {
        Long id = 1L;
        Integer qty = 0;

        StepVerifier.create(service.checkAvailability(id, qty))
                .verifyErrorSatisfies(ex -> {
                    assertThat(ex)
                            .isInstanceOf(BadRequestException.class)
                            .hasMessage("La cantidad solicitada debe ser mayor a 0.");
                });
    }

    @Test
    void checkAvailability_whenStockSufficientAndActive_shouldReturnTrue() {
        Long id = 1L;
        Integer qty = 3;
        Product product = new Product(id, "Monitor", 500.0, 10, true);

        when(repo.findById(id)).thenReturn(Mono.just(product));

        StepVerifier.create(service.checkAvailability(id, qty))
                .assertNext(available -> {
                    assertThat(available).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void checkAvailability_whenStockInsufficientOrInactive_shouldReturnFalse() {
        Long id = 1L;
        Integer qty = 5;
        Product inactive = new Product(id, "Monitor", 500.0, 10, false);
        Product insufficient = new Product(id, "Monitor", 500.0, 3, true);

        // Caso 1: inactivo
        when(repo.findById(id)).thenReturn(Mono.just(inactive));

        StepVerifier.create(service.checkAvailability(id, qty))
                .assertNext(available -> assertThat(available).isFalse())
                .verifyComplete();

        // Caso 2: stock insuficiente
        when(repo.findById(id)).thenReturn(Mono.just(insufficient));

        StepVerifier.create(service.checkAvailability(id, qty))
                .assertNext(available -> assertThat(available).isFalse())
                .verifyComplete();
    }

    @Test
    void decreaseStock_withInvalidQty_shouldErrorWithBadRequest() {
        Long id = 1L;
        Integer qty = 0;

        StepVerifier.create(service.decreaseStock(id, qty))
                .verifyErrorSatisfies(ex -> {
                    assertThat(ex)
                            .isInstanceOf(BadRequestException.class)
                            .hasMessage("La cantidad a descontar debe ser mayor a 0.");
                });
    }

    @Test
    void decreaseStock_whenProductInactive_shouldErrorWithBadRequest() {
        Long id = 1L;
        Integer qty = 1;
        Product inactive = new Product(id, "Impresora", 300.0, 10, false);

        when(repo.findById(id)).thenReturn(Mono.just(inactive));

        StepVerifier.create(service.decreaseStock(id, qty))
                .verifyErrorSatisfies(ex -> {
                    assertThat(ex)
                            .isInstanceOf(BadRequestException.class)
                            .hasMessage("El producto no está disponible para la venta.");
                });
    }

    @Test
    void decreaseStock_whenStockInsufficient_shouldErrorWithBadRequest() {
        Long id = 1L;
        Integer qty = 10;
        Product product = new Product(id, "Impresora", 300.0, 5, true);

        when(repo.findById(id)).thenReturn(Mono.just(product));

        StepVerifier.create(service.decreaseStock(id, qty))
                .verifyErrorSatisfies(ex -> {
                    assertThat(ex)
                            .isInstanceOf(BadRequestException.class)
                            .hasMessage("No hay stock suficiente para completar la operación.");
                });
    }

    @Test
    void decreaseStock_whenStockSufficientAndRemainingPositive_shouldUpdateStockAndKeepActive() {
        Long id = 1L;
        Integer qty = 2;
        Product product = new Product(id, "Impresora", 300.0, 5, true);

        when(repo.findById(id)).thenReturn(Mono.just(product));
        when(repo.save(any(Product.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.decreaseStock(id, qty))
                .assertNext(updated -> {
                    assertThat(updated.id()).isEqualTo(id);
                    assertThat(updated.stock()).isEqualTo(3);
                    assertThat(updated.active()).isTrue();
                })
                .verifyComplete();
    }

    @Test
    void decreaseStock_whenStockBecomesZero_shouldSetActiveFalse() {
        Long id = 1L;
        Integer qty = 5;
        Product product = new Product(id, "Impresora", 300.0, 5, true);

        when(repo.findById(id)).thenReturn(Mono.just(product));
        when(repo.save(any(Product.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.decreaseStock(id, qty))
                .assertNext(updated -> {
                    assertThat(updated.stock()).isEqualTo(0);
                    assertThat(updated.active()).isFalse();
                })
                .verifyComplete();
    }
}

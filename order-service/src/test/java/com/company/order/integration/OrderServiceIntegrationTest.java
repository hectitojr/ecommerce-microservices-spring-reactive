package com.company.order.integration;

import com.company.order.dto.CreateOrderRequest;
import com.company.order.model.OrderStatus;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderServiceIntegrationTest {

    private static WireMockServer wireMockServer;

    @LocalServerPort
    int port;

    private WebTestClient webTestClient;

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void afterAll() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("product-service.base-url",
                () -> "http://localhost:" + wireMockServer.port());
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void createOrder_whenStockAvailable_shouldCreateOrderWithStatusCreated() {
        Long productId = 1L;
        int qty = 2;

        stubFor(get(urlPathEqualTo("/products/" + productId + "/availability"))
                .withQueryParam("qty", equalTo(String.valueOf(qty)))
                .willReturn(ok()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("true")));

        stubFor(patch(urlPathEqualTo("/products/" + productId + "/stock/decrease"))
                .withQueryParam("qty", equalTo(String.valueOf(qty)))
                .willReturn(okJson("""
                        {
                          "id": 1,
                          "name": "Laptop",
                          "price": 1500.0,
                          "stock": 8,
                          "active": true
                        }
                        """)));

        CreateOrderRequest req = new CreateOrderRequest(productId, qty);

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.productId").isEqualTo(productId.intValue())
                .jsonPath("$.quantity").isEqualTo(qty)
                .jsonPath("$.total").isEqualTo(1500.0 * qty)
                .jsonPath("$.status").isEqualTo(OrderStatus.CREATED.name());

        verify(1, getRequestedFor(urlPathEqualTo("/products/" + productId + "/availability"))
                .withQueryParam("qty", equalTo(String.valueOf(qty))));
        verify(1, patchRequestedFor(urlPathEqualTo("/products/" + productId + "/stock/decrease"))
                .withQueryParam("qty", equalTo(String.valueOf(qty))));
    }

    @Test
    void createOrder_whenStockNotAvailable_shouldCreateOrderWithStatusRejected() {
        Long productId = 1L;
        int qty = 5;

        stubFor(get(urlPathEqualTo("/products/" + productId + "/availability"))
                .withQueryParam("qty", equalTo(String.valueOf(qty)))
                .willReturn(ok()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("false")));

        CreateOrderRequest req = new CreateOrderRequest(productId, qty);

        webTestClient.post()
                .uri("/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isNumber()
                .jsonPath("$.productId").isEqualTo(productId.intValue())
                .jsonPath("$.quantity").isEqualTo(qty)
                .jsonPath("$.total").isEqualTo(0.0)
                .jsonPath("$.status").isEqualTo(OrderStatus.REJECTED.name());

        verify(1, getRequestedFor(urlPathEqualTo("/products/" + productId + "/availability"))
                .withQueryParam("qty", equalTo(String.valueOf(qty))));
        verify(0, patchRequestedFor(urlPathEqualTo("/products/" + productId + "/stock/decrease")));
    }
}

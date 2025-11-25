package com.company.order.client;

import com.company.order.exception.BusinessException;
import com.company.order.exception.ExternalServiceException;
import com.company.order.exception.NotFoundException;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

class ProductClientTest {

    private static WireMockServer wireMockServer;
    private ProductClient productClient;

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

    @BeforeEach
    void setUp() {
        WebClient webClient = WebClient.builder()
                .baseUrl("http://localhost:" + wireMockServer.port())
                .build();
        productClient = new ProductClient(webClient);
        wireMockServer.resetAll();
    }

    @Test
    void checkAvailability_whenOk_shouldReturnBoolean() {
        Long productId = 1L;
        int qty = 5;

        stubFor(get(urlPathEqualTo("/products/" + productId + "/availability"))
                .withQueryParam("qty", equalTo(String.valueOf(qty)))
                .willReturn(ok()
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("true")));

        StepVerifier.create(productClient.checkAvailability(productId, qty))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void checkAvailability_whenNotFound_shouldThrowNotFoundException() {
        Long productId = 99L;
        int qty = 1;

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                "El producto solicitado no existe."
        );

        stubFor(get(urlPathEqualTo("/products/" + productId + "/availability"))
                .withQueryParam("qty", equalTo(String.valueOf(qty)))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "type": "%s",
                                  "title": "%s",
                                  "status": 404,
                                  "detail": "%s"
                                }
                                """.formatted(
                                pd.getType(),
                                pd.getTitle(),
                                pd.getDetail()
                        ))));

        StepVerifier.create(productClient.checkAvailability(productId, qty))
                .verifyErrorSatisfies(ex -> {
                    Assertions.assertInstanceOf(NotFoundException.class, ex);
                    Assertions.assertEquals(
                            "El producto no existe o no está disponible.",
                            ex.getMessage()
                    );
                });
    }

    @Test
    void decreaseStock_whenBusinessError_shouldThrowBusinessException() {
        Long productId = 1L;
        int qty = 100;

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "No hay stock suficiente para completar la operación."
        );

        stubFor(patch(urlPathEqualTo("/products/" + productId + "/stock/decrease"))
                .withQueryParam("qty", equalTo(String.valueOf(qty)))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "type": "%s",
                                  "title": "%s",
                                  "status": 400,
                                  "detail": "%s"
                                }
                                """.formatted(
                                pd.getType(),
                                pd.getTitle(),
                                pd.getDetail()
                        ))));

        StepVerifier.create(productClient.decreaseStock(productId, qty))
                .verifyErrorSatisfies(ex -> {
                    Assertions.assertInstanceOf(BusinessException.class, ex);
                    Assertions.assertTrue(
                            ex.getMessage().contains("Error de negocio al descontar el stock del producto")
                    );
                });
    }

    @Test
    void decreaseStock_whenServerError_shouldThrowExternalServiceException() {
        Long productId = 1L;
        int qty = 1;

        stubFor(patch(urlPathEqualTo("/products/" + productId + "/stock/decrease"))
                .withQueryParam("qty", equalTo(String.valueOf(qty)))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "type": "about:blank",
                                  "title": "ERROR_INTERNO",
                                  "status": 500,
                                  "detail": "Error interno en Product Service"
                                }
                                """)));

        StepVerifier.create(productClient.decreaseStock(productId, qty))
                .verifyErrorSatisfies(ex -> {
                    Assertions.assertInstanceOf(ExternalServiceException.class, ex);
                    Assertions.assertTrue(
                            ex.getMessage().contains("El servicio de productos presentó un error al descontar el stock del producto")
                    );
                });
    }

    @Test
    void checkAvailability_whenBadRequestFromProductService_shouldMapToBadRequestOrBusiness() {
        Long productId = 1L;
        int qty = -1;

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "La cantidad solicitada debe ser mayor a 0."
        );

        stubFor(get(urlPathEqualTo("/products/" + productId + "/availability"))
                .withQueryParam("qty", equalTo(String.valueOf(qty)))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .withBody("""
                                {
                                  "type": "%s",
                                  "title": "%s",
                                  "status": 400,
                                  "detail": "%s"
                                }
                                """.formatted(
                                pd.getType(),
                                pd.getTitle(),
                                pd.getDetail()
                        ))));

        StepVerifier.create(productClient.checkAvailability(productId, qty))
                .verifyErrorSatisfies(ex -> {
                    Assertions.assertInstanceOf(BusinessException.class, ex);
                    Assertions.assertTrue(
                            ex.getMessage().contains("Error de negocio al verificar la disponibilidad del producto")
                    );
                });
    }
}

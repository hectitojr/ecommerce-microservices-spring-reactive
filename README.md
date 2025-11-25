\# Prueba técnica – Microservicios Productos y Ordenes



Este proyecto implementa una solución basada en microservicios para gestionar productos y órdenes de compra, usando Spring Boot WebFlux y acceso reactivo a base de datos con R2DBC + H2.



El enfoque está pensado como una prueba técnica, pero siguiendo buenas prácticas de diseño, pruebas automatizadas e integración entre servicios.



---



\## Arquitectura general



El sistema está compuesto por dos microservicios:



\- `product-service`  

&nbsp; - Gestiona el catálogo de productos.

&nbsp; - Expone endpoints para crear, listar y consultar productos.

&nbsp; - Persiste datos en una base de datos H2 (R2DBC).



\- `order-service`  

&nbsp; - Gestiona la creación de órdenes de compra.

&nbsp; - Valida el stock del producto consultando `product-service`.

&nbsp; - Calcula el total de la orden.

&nbsp; - Persiste datos en una base de datos H2 (R2DBC).

&nbsp; - Incluye pruebas de integración utilizando WebTestClient y WireMock para simular el `product-service`.



La comunicación entre microservicios es vía HTTP (REST) y se realiza de forma reactiva usando WebClient.



---



\## Tecnologías utilizadas



\- Java 17 

\- Spring Boot 4.0.0  

&nbsp; - Spring WebFlux  

&nbsp; - Spring Data R2DBC  

\- H2 (base de datos embebida, modo archivo)

\- R2DBC para acceso reactivo a la base de datos

\- JUnit 5 para testing

\- Spring WebTestClient para pruebas de integración

\- WireMock para simular el `product-service` en las pruebas del `order-service`

\- Maven para la gestión de dependencias y build


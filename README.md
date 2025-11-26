Prueba Técnica – Microservicios de Productos y Órdenes

Java 17 • Spring Boot WebFlux 4.0.0 • R2DBC • H2 • WebClient • WireMock

Este proyecto implementa una arquitectura basada en microservicios para la gestión de productos y órdenes de compra.
Fue desarrollado como parte de una prueba técnica, priorizando diseño limpio, reactividad, buena comunicación entre servicios, y pruebas automatizadas.

Arquitectura General

La solución está compuesta por dos microservicios independientes:

1. product-service

Responsable del catálogo de productos.

Funcionalidades:

Crear productos

Listar productos activos

Consultar producto por ID

Modificar producto

Validar disponibilidad de stock

Descontar stock

2. order-service

Orquestador de creación de órdenes.

Funcionalidades:

Validar solicitud de orden

Consultar disponibilidad de producto en product-service

Descontar stock de forma transaccional via WebClient

Calcular total

Persistir orden

Consultar orden por ID

Listar órdenes

Comunicación entre Microservicios

Ambos servicios se comunican mediante REST reactivo usando WebClient.

order-service  --->  GET /products/{id}/availability
order-service  --->  PATCH /products/{id}/stock/decrease

Tecnologías Utilizadas:
Backend	Java 17, Spring Boot 4.0.0
Web	Spring WebFlux (reactivo)
BD	H2 (modo archivo), R2DBC
Pruebas	JUnit 5, WebTestClient, Mockito, WireMock
Build	Maven
Logs	SLF4J + Logback

Endpoints Principales

product-service:

Crear producto

POST /products

Listar productos activos

GET /products

Consultar por ID

GET /products/{id}

Actualizar producto

PUT /products/{id}

Validar disponibilidad (uso para order-service)

GET /products/{id}/availability?qty=2

Respuesta: true / false

GET /products/{id}/availability/details?qty=2

endpoint detallado de disponibilidad de stock

Ejemplo si hay stock:

{
"productId": 1,
"name": "Laptop Gamer",
"requestedQty": 2,
"currentStock": 12,
"available": true,
"message": "Existen 12 unidades disponibles en stock."
}



Ejemplo si NO hay suficiente stock:

{
"productId": 1,
"name": "Laptop Gamer",
"requestedQty": 5,
"currentStock": 3,
"available": false,
"message": "No hay stock suficiente del producto para la cantidad solicitada. Stock actual: 3"
}



Ejemplo si stock = 0:

{
"productId": 1,
"name": "Laptop Gamer",
"requestedQty": 1,
"currentStock": 0,
"available": false,
"message": "No hay stock del producto."
}

order-service

Crear orden

POST /orders

Ejemplo de request:

{
"productId": 1,
"quantity": 2
}

Consultar orden

GET /orders/{id}

Listar órdenes

GET /orders

Flujo de negocio de creación de órdenes

El cliente solicita crear una orden.

order-service valida parámetros.

Consulta en product-service la disponibilidad:

GET /products/{id}/availability?qty=x



Si no hay stock → BadRequestException (NO se crea orden).

Si hay stock → order-service solicita descuento:

PATCH /products/{id}/stock/decrease?qty=x



product-service actualiza stock.

order-service crea la orden en BD con estado CREATED.

Testing
Pruebas unitarias:

Validación de reglas de negocio

Validaciones de parámetros

Calculo de total

Pruebas de integración:

WebClient + WireMock (simulación del product-service)

WebTestClient para endpoints REST

Ejecución del proyecto:

1. Levantar product-service
   mvn spring-boot:run



Puerto por defecto: 8081

2. Levantar order-service
   mvn spring-boot:run



Puerto por defecto: 8082



Colecciones de Postman

Las colecciones utilizadas para probar los microservicios se encuentran en el directorio:



/Postman



Incluyen:



La colección completa de endpoints



Folders para cada microservicio



Ejemplos de solicitudes y respuestas



Estas colecciones pueden importarse directamente en Postman para facilitar las pruebas.



Autor

Ronald Urbano Miguel Chinchay Zelada
Backend Java / Arquitectura de Microservicios / Spring WebFlux


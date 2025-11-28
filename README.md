🧪 Prueba Técnica – Microservicios de Productos y Órdenes

Stack: Java 17 • Spring Boot 4.0.0 (WebFlux) • R2DBC • H2 (file) • WebClient • WireMock • Maven

Este proyecto implementa una arquitectura basada en microservicios para la gestión de productos y órdenes de compra.  
Fue desarrollado como parte de una prueba técnica, priorizando:

- ✅ Diseño limpio y capas bien separadas  
- ⚡ Programación reactiva (Spring WebFlux + R2DBC)  
- 🔁 Comunicación robusta entre microservicios  
- 🧪 Buen nivel de pruebas automatizadas (unitarias e integración)
_______________________________________________________________________________________________
🏗️ Arquitectura General

La solución está compuesta por dos microservicios independientes:

### 🧩 `product-service` (Catálogo de productos)

Responsable de la gestión del catálogo y el control de stock.

**Funcionalidades principales:**

- ➕ Crear productos  
- 📃 Listar productos **activos**  
- 🔍 Consultar producto por **ID**  
- ✏️ Modificar producto  
- ✅ Validar **disponibilidad de stock**  
- ➖ Descontar stock

### 📦 `order-service` (Órdenes de compra)

Actúa como **orquestador** del proceso de creación de órdenes.

**Funcionalidades principales:**

- ✅ Validar la solicitud de orden (parámetros y reglas de negocio)  
- 🔗 Consultar disponibilidad del producto en `product-service`  
- 🔁 Descontar stock en `product-service` vía **WebClient**  
- 💰 Calcular el total de la orden  
- 💾 Persistir la orden en BD  
- 🔍 Consultar orden por **ID**  
- 📃 Listar todas las órdenes
_______________________________________________________________________________________________
## 📡 Comunicación entre Microservicios

La comunicación entre servicios se realiza vía **REST reactivo** usando `WebClient`:

- `order-service` → `product-service`
  
GET  /products/{id}/availability?qty=x
PATCH /products/{id}/stock/decrease?qty=x

Toda la comunicación es no bloqueante, alineada con Spring WebFlux.
_______________________________________________________________________________________________
⚙️ Tecnologías Utilizadas
🖥️ Backend

☕ Java 17

🚀 Spring Boot 4.0.0

⚡ Spring WebFlux (endpoints y flujos reactivos)

🗄️ H2 Database en modo archivo

🔄 R2DBC (acceso a datos reactivo)

🔌 Comunicación HTTP

🌐 WebClient (cliente HTTP reactivo)

🧪 Testing

🧱 JUnit 5

🌊 Reactor Test (StepVerifier)

🌐 WebTestClient (pruebas de endpoints WebFlux)

🎭 Mockito (mocks de servicios/repositorios)

🎯 WireMock (simulación de product-service en pruebas de integración)

🔨 Build & Logging

🧰 Maven

📜 SLF4J + Logback (logging estructurado)
_______________________________________________________________________________________________
🌐 Endpoints Principales
🧩 product-service
➕ Crear producto

POST /products

{
  "name": "Laptop",
  "price": 1500.0,
  "stock": 10
}
`_______________________________________________________________________________________________`
📃 Listar productos activos

GET /products
`_______________________________________________________________________________________________`
🔍 Consultar producto por ID

GET /products/{id}
`_______________________________________________________________________________________________`
✏️ Actualizar producto

PUT /products/{id}

{
  "name": "Laptop Gamer",
  "price": 2000.0,
  "stock": 8,
  "active": true
}
`_______________________________________________________________________________________________`
✅ Validar disponibilidad (usado por order-service)

GET /products/{id}/availability?qty=2
📥 Respuesta: true / false
`_______________________________________________________________________________________________`
🔍 Detalle de disponibilidad

GET /products/{id}/availability/details?qty=2

Ejemplo – hay stock suficiente:

{
  "productId": 1,
  "name": "Laptop Gamer",
  "requestedQty": 2,
  "currentStock": 12,
  "available": true,
  "message": "Existen 12 unidades disponibles en stock."
}

Ejemplo – NO hay stock suficiente:

{
  "productId": 1,
  "name": "Laptop Gamer",
  "requestedQty": 5,
  "currentStock": 3,
  "available": false,
  "message": "No hay stock suficiente del producto para la cantidad solicitada. Stock actual: 3"
}

Ejemplo – stock agotado (currentStock = 0):

{
  "productId": 1,
  "name": "Laptop Gamer",
  "requestedQty": 1,
  "currentStock": 0,
  "available": false,
  "message": "No hay stock del producto."
}
`_______________________________________________________________________________________________`
📦 order-service
🧾 Crear orden

POST /orders

Ejemplo de request:

{
  "productId": 1,
  "quantity": 2
}
`_______________________________________________________________________________________________`
🔍 Consultar orden por ID

GET /orders/{id}
`_______________________________________________________________________________________________`
📃 Listar órdenes

GET /orders
_______________________________________________________________________________________________
🔁 Flujo de Negocio: Creación de Orden

👤 El cliente envía una solicitud de creación de orden:

POST /orders con { productId, quantity }.

🧩 order-service:

Valida parámetros (id > 0, quantity > 0).

Llama a product-service para validar stock:

GET /products/{id}/availability?qty=x

📦 Si no hay stock suficiente:

Se lanza una BadRequestException a nivel de order-service.

La orden no se crea / o se maneja como REJECTED según la lógica de negocio configurada.

📦 Si hay stock suficiente:

order-service solicita descontar stock:

PATCH /products/{id}/stock/decrease?qty=x

product-service actualiza el stock y devuelve el producto actualizado (con su precio).

💰 order-service calcula el total: precio * cantidad.

💾 Se persiste la orden en la BD con estado CREATED.
_______________________________________________________________________________________________
🧪 Testing
✅ Pruebas Unitarias

✔️ Validación de reglas de negocio:

Cantidad > 0

ID de producto > 0

Manejo de stock y estado activo/inactivo

✔️ Cálculo de total en order-service

✔️ Validación de estados de orden (CREATED, REJECTED)

✔️ Manejo de excepciones de dominio (BadRequestException, NotFoundException, BusinessException, ExternalServiceException)

Herramientas usadas:

JUnit 5

Mockito (mocks de repositorios y clientes HTTP)

StepVerifier (verificación de flujos Mono/Flux)

AssertJ para assertions expresivas
_______________________________________________________________________________________________
🔗 Pruebas de Integración

🌐 WebTestClient para probar endpoints REST de forma reactiva.

🎯 WireMock para simular product-service en escenarios como:

Stock disponible / no disponible

Errores 4xx / 5xx desde el servicio externo

🔄 Validación de:

Flujo completo order-service → product-service → BD H2

Contratos HTTP (estatus, body, manejo de errores)
_______________________________________________________________________________________________
▶️ Ejecución del Proyecto
🧩 Levantar product-service
cd product-service
mvn spring-boot:run

🔌 Puerto por defecto: 8081
_______________________________________________________________________________________________
📦 Levantar order-service
cd order-service
mvn spring-boot:run


🔌 Puerto por defecto: 8082
_______________________________________________________________________________________________
🧰 Colecciones de Postman

Las colecciones utilizadas para probar los microservicios se encuentran en el directorio:

/Postman

Incluyen:

📂 Colección completa de endpoints

📁 Folders para cada microservicio (product-service, order-service)

📄 Ejemplos de solicitudes y respuestas listas para usar

Pueden importarse directamente en Postman para facilitar las pruebas manuales.
_______________________________________________________________________________________________
👨‍💻 Autor

Ronald Urbano Miguel Chinchay Zelada
Backend Java • Arquitectura de Microservicios • Spring WebFlux

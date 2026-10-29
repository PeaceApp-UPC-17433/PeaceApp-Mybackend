# PeaceApp Backend

This repository contains the backend implementation for the PeaceApp application. The backend is built using 
Java and Spring Boot, providing RESTful services for managing alerts, user profiles, reports, and location data.

## Features
- **User Management**: Create and manage user profiles securely.
- **Alert System**: Allow users to create and respond to alerts.
- **Location Tracking**: Record and manage user locations.
- **Incident Reporting**: Manage and view reported incidents.

## Project Structure
The project follows a standard Maven-based Spring Boot structure with a clear separation of concerns:

- **Application Entry Point**:
  - `PeaceappApplication.java`: Main class that bootstraps the Spring Boot application.

- **Controllers** (`src/main/java/com/upc/pre/peaceapp/controllers`):
  - `AlertController.java`: Handles HTTP requests related to alerts.
  - `LocationController.java`: Manages location-related requests.
  - `ReportController.java`: Handles all operations related to reports.
  - `UserController.java`: Manages user-related requests.

- **Models** (`src/main/java/com/upc/pre/peaceapp/models`):
  - Classes representing the entities of the application, such as `UserProfile`, `Alert`, `Location`, and `Report`.

- **Repositories** (`src/main/java/com/upc/pre/peaceapp/repositories`):
  - Interfaces extending `JpaRepository` for CRUD operations on the entities.

- **Resources** (`src/main/resources`):
  - `application.properties`: Defines configuration settings such as database connection, server ports, etc.

## Getting Started

### Prerequisites
- **JDK 11 or 17** (preferably)
- **Maven 3.6+**
- **IntelliJ IDEA** or any Java IDE

### Installation
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/username/PeaceAppBackend.git
   cd PeaceAppBackend
   ```
2. **Import the Project in IntelliJ**:
   - Open IntelliJ IDEA, click on **Open** and select the project's `pom.xml` file to load it as a Maven project.
3. **Build and Run**:
   - Let IntelliJ sync Maven dependencies.
   - Run the `PeaceappApplication` class to start the backend server.

### Usage
- The backend provides RESTful endpoints that can be accessed using tools like Postman or via the frontend application.
- The base URL for the API is `http://localhost:8080`.

---

## API por fases

Registro de los endpoints que se van agregando o cambiando en cada fase del plan (ver `ANALISIS_FUNCIONAL_BACKEND.md`). Sirve como referencia para la integración con el frontend. Cada fase añade su sección; las fases futuras se marcan como pendientes.

### Convenciones generales

- **Base URL**: `http://localhost:8080`
- **Autenticación**: las rutas protegidas piden el header `Authorization: Bearer <token>`. El token se obtiene en `sign-in`.
- **Rutas públicas**: `/api/v1/authentication/**` (excepto `change-password`), Swagger y OpenAPI.
- **Formato de error** (todos los errores de la Fase 1 en adelante):
  ```json
  { "message": "Hay campos inválidos", "errors": { "email": "El correo no tiene un formato válido" } }
  ```
  `errors` solo aparece cuando el error es de validación de campos.
- **Identificador de usuario**: el correo (en minúsculas). El JWT lleva el correo como identidad.

### Fase 1: Identidad, autenticación y perfil

Estado: implementada. Rama: `feature/unify-user-registration`.

| Método | Ruta | Dónde está | Qué hace | Auth | Respuestas |
|---|---|---|---|---|---|
| POST | `/api/v1/authentication/sign-up` | `security/iam/interfaces/rest/AuthIAMController` | Registro unificado: crea la cuenta y el perfil en una sola transacción. Siempre asigna `ROLE_USER` | Pública | `201` cuenta creada, `400` campos inválidos, `409` correo ya registrado |
| POST | `/api/v1/authentication/sign-in` | `AuthIAMController` | Inicia sesión y devuelve el JWT. El campo `username` lleva el correo | Pública | `200` con `token`, `400` campos inválidos, `401` "Credenciales incorrectas" |
| PUT | `/api/v1/authentication/change-password` | `AuthIAMController` | Cambia la contraseña del usuario autenticado, verificando la actual | Requiere token | `204`, `400` contraseña actual incorrecta o nueva inválida, `401` sin token |
| POST | `/api/v1/authentication/forgot-password` | `AuthIAMController` | Solicita recuperación. Genera un token temporal (30 min) y lo envía por `PasswordResetNotifier` | Pública | `202` siempre, exista o no el correo |
| POST | `/api/v1/authentication/reset-password` | `AuthIAMController` | Fija una contraseña nueva usando el token de recuperación (un solo uso) | Pública | `204`, `400` token inválido, vencido o usado |
| GET | `/api/v1/users/{email}` | `controllers/UserController` | Obtiene el perfil por correo | Requiere token, solo el dueño o admin | `200`, `403`, `404` |
| PUT | `/api/v1/users/{id}` | `UserController` | Actualización parcial del perfil (`name`, `lastname`, `phonenumber`, `email`, `profile_image`). Un campo ausente no se modifica. Si cambia `email`, también cambia el correo de inicio de sesión | Requiere token, solo el dueño o admin | `200`, `400` campos inválidos, `403`, `404`, `409` correo en uso |
| DELETE | `/api/v1/users/{id}` | `UserController` | Elimina el perfil y la cuenta de acceso de forma atómica | Requiere token, solo el dueño o admin | `204`, `403`, `404` |

`{id}` en `/users/{id}` es el id del perfil (campo `id` del `GET /users/{email}`), no el de la cuenta de acceso.

**Cuerpos de petición**

| Endpoint | Campos |
|---|---|
| `sign-up` | `email` (obligatorio, máx. 50), `password` (obligatorio, 8 a 72), `name` (obligatorio, máx. 50), `lastname` (obligatorio, máx. 50), `phonenumber` (opcional, 6 a 20 caracteres entre dígitos, `+`, `-` y espacio) |
| `sign-in` | `username` (el correo), `password` |
| `change-password` | `currentPassword`, `newPassword` (8 a 72) |
| `forgot-password` | `email` |
| `reset-password` | `token`, `newPassword` (8 a 72) |
| `PUT /users/{id}` | `name`, `lastname`, `phonenumber`, `email`, `profile_image`, todos opcionales |

**Cambios que rompen compatibilidad con el frontend**

| Antes | Ahora |
|---|---|
| `sign-up` con `username`, `password` y `roles` | `sign-up` con `email`, `password`, `name`, `lastname`, `phonenumber`. Ya no acepta `roles` |
| `POST /api/v1/users` creaba el perfil | Eliminado. El perfil se crea en `sign-up` |
| `change-password` público, con `username` y `password` | Requiere token y pide `currentPassword` y `newPassword` |
| Errores en texto plano o con `200` | JSON uniforme con el código HTTP correcto |
| Cualquier usuario podía editar o borrar perfiles ajenos | Solo el dueño o un admin |

Notas:
- Tras cambiar el correo con `PUT /users/{id}`, el frontend debe volver a iniciar sesión para obtener un token nuevo.
- Mientras no exista un adaptador de correo, el token de recuperación se escribe en el log del servidor (solo desarrollo).
- No hay forma de crear usuarios admin todavía.

### Fase 2: Reportes y mapa

Estado: implementada. Rama: `feature/report-domain-expansion`.

Vive en su propio bounded context, `reports/`, con las mismas capas que IAM: `domain` (agregado `Report`, value objects, commands, queries), `application` (servicios de comandos y consultas, ACL hacia usuarios), `infrastructure` (JPA) e `interfaces/rest`. Las coordenadas son parte del reporte (`GeoPoint`); `/locations` quedó como vista de compatibilidad sobre los reportes.

Todos los endpoints piden token. El usuario que reporta sale del token, ya no se envía en el body.

| Método | Ruta | Qué hace | Quién | Respuestas |
|---|---|---|---|---|
| POST | `/api/v1/reports` | Crea un reporte (estado `PENDING`) | Cualquier usuario | `201` con `Location`, `400` campos inválidos |
| GET | `/api/v1/reports` | Lista paginada con filtros | Cualquier usuario | `200`, `400` parámetro inválido |
| GET | `/api/v1/reports/{id}` | Obtiene un reporte | Cualquier usuario | `200`, `404` |
| GET | `/api/v1/reports/user/{userId}` | Reportes de un perfil, paginado (equivale a `?userId=`) | Cualquier usuario | `200` |
| PUT | `/api/v1/reports/{id}` | Edición parcial (también mueve el reporte con `latitude` y `longitude`) | Dueño o admin | `200`, `400`, `403`, `404` |
| PUT | `/api/v1/reports/{id}/status` | Cambia el estado | Solo admin | `200`, `403`, `404`, `409` transición no permitida |
| DELETE | `/api/v1/reports/{id}` | Elimina el reporte | Dueño o admin | `204`, `403`, `404` |
| GET | `/api/v1/reports/map` | Marcadores del área visible, sin datos pesados | Cualquier usuario | `200`, `400` área inválida |
| GET | `/api/v1/reports/heatmap` | Mapa de calor por cuadrícula con nivel de riesgo | Cualquier usuario | `200`, `400` |
| POST | `/api/v1/locations` | Compatibilidad: mueve un reporte existente a unas coordenadas | Dueño o admin | `201`, `400`, `403`, `404` |
| GET | `/api/v1/locations` | Compatibilidad: ubicaciones de los últimos 1000 reportes | Cualquier usuario | `200` |
| GET | `/api/v1/locations/dangerous?quantity_reports=N` | Compatibilidad: zonas con al menos N reportes activos | Cualquier usuario | `200`, `400` |

**Cuerpo de `POST /reports`**: `title` (obligatorio, máx. 50), `detail` (opcional, máx. 255), `type` (obligatorio), `severity` (opcional, por defecto `MEDIUM`), `occurredAt` (opcional, ISO-8601, por defecto ahora, no puede ser futura), `latitude` (-90 a 90), `longitude` (-180 a 180), `address` (obligatorio, máx. 100), `image` (opcional, máx. 500). `PUT /reports/{id}` acepta los mismos campos, todos opcionales, pero `latitude` y `longitude` deben ir juntas.

**Valores**
- `type`: `THEFT`, `ACCIDENT`, `LIGHTING`, `HARASSMENT`, `EMERGENCY`, `OTHER`.
- `severity`: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`.
- `status`: `PENDING`, `UNDER_REVIEW`, `VERIFIED`, `DISCARDED`, `RESOLVED`. Transiciones: `PENDING` a `UNDER_REVIEW`, `VERIFIED` o `DISCARDED`; `UNDER_REVIEW` a `VERIFIED` o `DISCARDED`; `VERIFIED` a `RESOLVED` o `DISCARDED`. `DISCARDED` y `RESOLVED` son finales y dejan de contar en el mapa de calor.

**Filtros de `GET /reports`** (combinables): `type`, `severity`, `status`, `from` y `to` (ISO-8601 sobre `occurredAt`), `q` (texto en título, detalle o dirección), `userId`, `mine=true` (reportes del usuario del token). Paginación: `page` (desde 0), `size` (1 a 100, por defecto 20), `sortBy` (`occurredAt` o `createdAt`) y `direction` (`asc` o `desc`). La respuesta es `{ content, page, size, totalElements, totalPages }`.

**Área de `GET /reports/map`**: un rectángulo (`minLat`, `maxLat`, `minLng`, `maxLng`) o un círculo (`lat`, `lng`, `radiusMeters`, máx. 50000), no ambos. Acepta los mismos filtros de tipo, severidad, estado y fechas, más `limit` (1 a 1000, por defecto 500; devuelve los más recientes). Cada punto trae `id`, `latitude`, `longitude`, `type` y `severity`.

**`GET /reports/heatmap`**: parámetros opcionales `minLat`, `maxLat`, `minLng`, `maxLng` (los cuatro o ninguno), `cellSize` en grados (0.0005 a 0.5, por defecto 0.005, unos 550 m) y `minCount` (por defecto 1). Devuelve `{ latitude, longitude, count, riskLevel }` con el centro de cada celda, de más a menos reportes. `riskLevel`: 1-2 reportes `LOW`, 3-5 `MEDIUM`, 6-9 `HIGH`, 10 o más `CRITICAL`.

**Cambios que rompen compatibilidad con el frontend**

| Antes | Ahora |
|---|---|
| `POST /reports` con `user_id` en el body | Se ignora; el dueño es el usuario del token |
| `type` texto libre | Enum (`THEFT`, `ACCIDENT`, ...) |
| Reporte sin ubicación; ubicación aparte con `POST /locations` | `latitude` y `longitude` obligatorias al crear el reporte |
| `GET /reports` devolvía un arreglo con todo | Objeto paginado `{ content, ... }` |
| `GET /reports/user/{userId}` devolvía un arreglo | Objeto paginado |
| Reporte inexistente: `400` | `404` |
| Cualquier usuario borraba cualquier reporte | Solo el dueño o un admin |
| Respuesta con `alatitude` y `alongitude` en ubicaciones | `latitude` y `longitude` |
| `POST /locations` creaba una ubicación | Mueve el reporte `idReport` (debe existir y ser tuyo) |
| `GET /locations/dangerous` devolvía todas las ubicaciones | Devuelve celdas `{ latitude, longitude, count, riskLevel }` |

Notas:
- `idUser` del reporte es el id del perfil (el mismo `id` de `GET /users/{email}`).
- Al arrancar contra una base MySQL con datos anteriores, `ddl-auto=update` añade las columnas nuevas, pero los reportes viejos tienen `type` en texto libre y quedan sin `severity`, `status` ni coordenadas válidas. Hay que migrarlos o vaciar la tabla `reports`. La tabla `locations` ya no se usa.
- Las alertas (`/api/v1/alerts`) siguen igual hasta la Fase 3.

### Fase 3: Alertas y notificaciones

Estado: pendiente.

### Fase 4: Comunidad y confianza

Estado: pendiente.

### Fase 5: Funciones avanzadas

Estado: pendiente.

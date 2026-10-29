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

Estado: pendiente.

### Fase 3: Alertas y notificaciones

Estado: pendiente.

### Fase 4: Comunidad y confianza

Estado: pendiente.

### Fase 5: Funciones avanzadas

Estado: pendiente.

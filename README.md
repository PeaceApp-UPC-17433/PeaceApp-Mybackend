# PeaceApp Backend

This repository contains the backend implementation for the PeaceApp application. The backend is built using 
Java and Spring Boot, providing RESTful services for managing alerts, user profiles, reports, and location data.

## Features
- **Identidad y perfil**: registro unificado, inicio de sesión con JWT, recuperación de contraseña y edición del perfil.
- **Reportes y mapa**: reportes con tipo, severidad y estado, filtros, paginación, consulta por área y mapa de calor.
- **Alertas y notificaciones**: avisos por proximidad, historial, preferencias y push por puerto.
- **Comunidad y moderación**: comentarios, votos de confirmación, verificación, denuncias de abuso y panel de administración.

## Project Structure
El proyecto sigue DDD con un bounded context por carpeta. Cada contexto repite las mismas capas
(`domain`, `application`, `infrastructure`, `interfaces/rest`) y se comunica con los demás por eventos de dominio
o por una capa anticorrupción (`application/internal/outboundservices/acl`).

Bajo `src/main/java/com/upc/pre/peaceapp`:

- `security/iam`: cuentas, roles, JWT y recuperación de contraseña.
- `controllers`, `models`, `repositories`, `services`: perfil de usuario (contexto anterior a la migración a DDD).
- `reports`: agregado `Report`, mapa y mapa de calor. `/locations` es una vista de compatibilidad.
- `alerts`: alertas por proximidad y su configuración.
- `notifications`: avisos dentro de la app, preferencias y dispositivos push.
- `community`: comentarios, votos, denuncias, historial de moderación y métricas.
- `shared`: manejo uniforme de errores, paginación y documentación OpenAPI.

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
- Las alertas (`/api/v1/alerts`) se rehicieron en la Fase 3.

### Fase 3: Alertas y notificaciones

Estado: implementada. Rama: `feature/proximity-alert-engine`.

Dos bounded contexts nuevos con las mismas capas que IAM: `alerts/` y `notifications/`. Se comunican por eventos de dominio, sin tocar los repositorios del otro:

1. `POST /reports` publica `ReportCreatedEvent` (después de confirmar la transacción).
2. `alerts/` busca a los usuarios cuya última ubicación conocida esté dentro de su radio y crea una alerta para cada uno. Publica `AlertGeneratedEvent` por alerta.
3. `notifications/` convierte la alerta en un aviso dentro de la app y lo envía por push a los dispositivos registrados, respetando las preferencias.

Cada paso corre en su propia transacción: si falla la generación de alertas o el envío, el reporte (o la alerta) ya guardado no se ve afectado; el error queda en el log.

**Reglas de proximidad.** Se alerta a un usuario cuando se cumple todo esto: no es quien reportó, su última ubicación tiene menos de 24 horas, el reporte está dentro de su radio (por defecto 1000 m, de 100 a 20000), la severidad llega a su mínimo, el tipo no está ignorado, no tiene las alertas silenciadas y no tenía ya una alerta por ese reporte (hay a lo sumo una alerta por usuario y reporte). Los reportes que se editan o se mueven después no generan alertas nuevas.

Todos los endpoints piden token y trabajan sobre el usuario del token.

| Método | Ruta | Qué hace | Respuestas |
|---|---|---|---|
| PUT | `/api/v1/alerts/settings/location` | Guarda la última ubicación del usuario. El móvil debe enviarla cada cierto tiempo: sin ella no recibe alertas | `200`, `400` |
| GET | `/api/v1/alerts/settings` | Configuración de alertas (valores por defecto si nunca se guardó) | `200` |
| PUT | `/api/v1/alerts/settings` | Reemplaza la configuración | `200`, `400` |
| GET | `/api/v1/alerts` | Mis alertas, paginadas, de más nueva a más vieja. `unread=true` solo las no leídas | `200`, `400` |
| GET | `/api/v1/alerts/unread-count` | Cantidad de alertas sin leer | `200` |
| GET | `/api/v1/alerts/{id}` | Una alerta mía | `200`, `403`, `404` |
| GET | `/api/v1/alerts/user/{userId}` | Alertas de un perfil (id de perfil). Solo el dueño o admin | `200`, `403`, `404` perfil inexistente |
| PUT | `/api/v1/alerts/{id}/read` | Marca una alerta como leída | `200`, `403`, `404` |
| PUT | `/api/v1/alerts/read-all` | Marca todas mis alertas como leídas; devuelve `{ "count": N }` | `200` |
| DELETE | `/api/v1/alerts/{id}` | Borra una alerta mía | `204`, `403`, `404` |
| DELETE | `/api/v1/alerts` | Borra todas **mis** alertas (ya no borra las de todos) | `204` |
| GET | `/api/v1/notifications` | Historial de avisos, paginado. `unread=true` solo los no leídos | `200`, `400` |
| GET | `/api/v1/notifications/unread-count` | Avisos sin leer | `200` |
| PUT | `/api/v1/notifications/{id}/read` | Marca un aviso como leído | `200`, `403`, `404` |
| PUT | `/api/v1/notifications/read-all` | Marca todos como leídos | `200` |
| DELETE | `/api/v1/notifications/{id}` | Borra un aviso | `204`, `403`, `404` |
| GET | `/api/v1/notifications/preferences` | Canales y tipos de aviso activos | `200` |
| PUT | `/api/v1/notifications/preferences` | Reemplaza las preferencias | `200`, `400` |
| POST | `/api/v1/notifications/devices` | Registra el token push del dispositivo. Si el token ya era de otra cuenta, pasa a esta | `201`, `400` |
| DELETE | `/api/v1/notifications/devices?token=...` | Quita el dispositivo (solo si es del usuario) | `204` |

**Cuerpos de petición**

| Endpoint | Campos |
|---|---|
| `PUT /alerts/settings/location` | `latitude` (-90 a 90), `longitude` (-180 a 180), ambos obligatorios |
| `PUT /alerts/settings` | `radiusMeters` (obligatorio, 100 a 20000), `minSeverity` (obligatorio), `ignoredTypes` (opcional, tipos de reporte), `mutedUntil` (opcional, ISO-8601; si es futura, silencia hasta esa fecha). Es un reemplazo completo: lo que falta se interpreta como "ninguno" |
| `PUT /notifications/preferences` | `inAppEnabled` y `pushEnabled` (obligatorios), `disabledKinds` (opcional; hoy solo `PROXIMITY_ALERT`). Reemplazo completo |
| `POST /notifications/devices` | `token` (obligatorio, máx. 512), `platform` (`ANDROID`, `IOS` o `WEB`) |

**Respuesta de una alerta**: `id`, `reportId`, `recipientId`, `title`, `type`, `severity`, `latitude`, `longitude`, `distanceMeters`, `status` (`UNREAD` o `READ`), `createdAt`, `readAt`. Los datos del reporte son una copia de cuando se creó la alerta: el historial no cambia aunque el reporte se edite o se elimine.

**Cambios que rompen compatibilidad con el frontend**

| Antes | Ahora |
|---|---|
| `POST /alerts/` creaba una alerta a mano con `location`, `type`, `idUser`... | Eliminado. Las alertas las crea el sistema al publicarse un reporte cercano |
| `GET /alerts/` devolvía un arreglo con todas las alertas de todos (`204` si no había) | Objeto paginado `{ content, ... }` con las alertas del usuario del token (`200`, también vacío) |
| `GET /alerts/user/{userId}` devolvía un arreglo sin validar nada | Paginado; solo el dueño o admin; `404` si el perfil no existe |
| `DELETE /alerts/` borraba todas las alertas de todos y devolvía `200` | Borra solo las del usuario del token y devuelve `204` |
| Alerta inexistente: `400` | `404` |
| Campos de la alerta: `location`, `type` (texto), `description`, `idUser`, `imageUrl`, `idReport` | `title`, `type` (enum), `latitude`, `longitude`, `distanceMeters`, `status`, `recipientId`, `reportId` |

Notas:
- El envío push todavía no llega a un proveedor real: `LoggingPushNotificationSender` solo escribe en el log. Para activar Firebase Cloud Messaging basta con implementar `PushNotificationSender` (puerto en `notifications/application/internal/outboundservices/push`) con un `@Component` que reemplace al de log.
- Las alertas viven en la tabla nueva `user_alerts`. La tabla `alerts` de las fases anteriores ya no se usa; si tiene datos, no se migran.
- Las alertas de emergencia enviadas por el usuario y las alertas a contactos de confianza quedan para la Fase 5.

### Fase 4: Comunidad y confianza

Estado: implementada. Rama: `feature/report-comments-votes`.

Bounded context nuevo, `community/`, con las mismas capas que IAM. Tiene cuatro agregados: `ReportComment`, `ReportVote`, `AbuseFlag` y `ModerationAction`. Consulta a `reports/` solo por su ACL (`ExternalReportService`) y se comunica con él por eventos:

1. `PUT /reports/{id}/vote` guarda el voto. Si las confirmaciones netas (confirmaciones menos dudas) llegan al umbral, publica `ReportConfirmedByCommunityEvent`.
2. `reports/` reacciona (después de confirmar la transacción, en una propia) y pasa el reporte a `VERIFIED`, pero solo si seguía en `PENDING` o `UNDER_REVIEW`.
3. Al borrar un reporte, `reports/` publica `ReportDeletedEvent` y `community/` borra en la misma transacción sus comentarios, votos y denuncias. El historial de moderación se conserva.

Todos los endpoints piden token. Quien actúa sale del token.

| Método | Ruta | Qué hace | Quién | Respuestas |
|---|---|---|---|---|
| POST | `/api/v1/reports/{id}/comments` | Comenta un reporte | Cualquier usuario | `201`, `400`, `404` |
| GET | `/api/v1/reports/{id}/comments` | Comentarios visibles, paginados, del más antiguo al más nuevo | Cualquier usuario | `200`, `404` |
| DELETE | `/api/v1/comments/{id}` | Retira un comentario (borrado lógico) | Autor o admin | `204`, `403`, `404` |
| PUT | `/api/v1/reports/{id}/vote` | Crea o cambia mi voto (`CONFIRM` o `DISPUTE`) | Cualquier usuario menos el dueño del reporte | `200`, `400`, `403`, `404` |
| DELETE | `/api/v1/reports/{id}/vote` | Retira mi voto (idempotente) | Cualquier usuario | `200`, `404` |
| GET | `/api/v1/reports/{id}/votes` | Conteos, mi voto y umbral de verificación | Cualquier usuario | `200`, `404` |
| POST | `/api/v1/reports/{id}/flag` | Denuncia un reporte | Cualquier usuario menos el dueño | `201`, `400`, `403`, `404`, `409` ya denunciado |
| POST | `/api/v1/comments/{id}/flag` | Denuncia un comentario | Cualquier usuario menos el autor | `201`, `400`, `403`, `404`, `409` |
| GET | `/api/v1/admin/flags` | Denuncias, las más nuevas primero. Filtro `status` (`OPEN`, `DISMISSED`, `ACTION_TAKEN`) | Solo admin | `200`, `403` |
| PUT | `/api/v1/admin/flags/{id}/resolve` | Resuelve una denuncia | Solo admin | `200`, `400`, `403`, `404`, `409` ya resuelta |
| GET | `/api/v1/admin/moderation-log` | Historial de acciones de moderación | Solo admin | `200`, `403` |
| GET | `/api/v1/admin/metrics` | Métricas de calidad | Solo admin | `200`, `403` |

**Cuerpos de petición**

| Endpoint | Campos |
|---|---|
| comentario | `text` (obligatorio, máx. 500) |
| voto | `type` (`CONFIRM` o `DISPUTE`) |
| denuncia | `reason` (`SPAM`, `FALSE_INFORMATION`, `OFFENSIVE`, `DUPLICATE`, `OTHER`), `details` (opcional, máx. 255) |
| resolver denuncia | `resolution` (`DISMISSED` o `ACTION_TAKEN`), `note` (opcional, máx. 255) |

**Reglas**
- Un usuario tiene un solo voto por reporte; volver a votar lo reemplaza. No se vota un reporte propio ni uno ya descartado o resuelto.
- Un usuario denuncia un contenido una sola vez y no puede denunciar el suyo.
- `ACTION_TAKEN` sobre un comentario lo retira. Sobre un reporte lo pasa a `DISCARDED` (si ya estaba descartado o resuelto no cambia). Las demás denuncias abiertas sobre el mismo contenido se cierran con la misma resolución. `DISMISSED` no toca el contenido.
- Cada resolución, y cada comentario ajeno que borra un admin, queda en el historial de moderación (`COMMENT_REMOVED`, `REPORT_DISCARDED`, `FLAG_DISMISSED`).

**Respuesta de `GET /reports/{id}/votes`**: `confirmations`, `disputes`, `net`, `myVote` (nulo si no votaste) y `verificationThreshold`.

**Respuesta de `GET /admin/metrics`**: `reports` (`total`, `byStatus`, `verified` incluye resueltos, `discarded`, `verifiedRate`, `averageAttentionHours`), `community` (`comments`, `confirmations`, `disputes`) y `flags` (`open`, `dismissed`, `actionTaken`, `total`, `actionRate`). Las tasas y el promedio son `null` mientras no haya datos. El tiempo de atención es la diferencia entre la creación y el último cambio de los reportes verificados, descartados o resueltos (últimos 5000).

**Configuración**

| Propiedad | Por defecto | Qué hace |
|---|---|---|
| `community.verification.confirmation-threshold` | `3` | Confirmaciones netas que verifican un reporte |
| `peaceapp.admin.email` / `peaceapp.admin.password` | vacío (variables `ADMIN_EMAIL` y `ADMIN_PASSWORD`) | Si se definen, al arrancar se crea esa cuenta con `ROLE_ADMIN` (y su perfil) cuando no existe. La contraseña necesita al menos 8 caracteres |

**Cambios que rompen compatibilidad con el frontend**: ninguno. Los endpoints son nuevos y los existentes no cambian.

Notas:
- Ya hay forma de crear un administrador: la siembra de arriba. No hay rol de moderador aparte; la moderación es de `ROLE_ADMIN`.
- Quedan fuera de esta fase la suspensión de usuarios y la reputación. Van con los roles de la Fase 5.
- Los comentarios retirados siguen en la base (borrado lógico) para que las denuncias y el historial tengan a qué apuntar.
- Si el voto que alcanza el umbral se guarda pero falla la verificación, el error queda en el log y el reporte sigue como estaba.

### Fase 5: Funciones avanzadas

Estado: pendiente.

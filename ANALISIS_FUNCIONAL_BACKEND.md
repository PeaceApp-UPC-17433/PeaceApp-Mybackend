# Análisis funcional del backend de PeaceApp

Documento de trabajo para comparar lo que ofrece el informe del proyecto con lo que realmente hace el backend actual, y para dejar claro qué funcionalidades faltan y cuáles conviene ampliar.

El enfoque es funcional: qué puede hacer el usuario, qué puede hacer el sistema y qué flujos todavía no están cubiertos. No se entra en CI/CD, despliegue, pruebas automatizadas ni refactorización interna, salvo cuando sea necesario para explicar una funcionalidad.

---

## 1. Resumen ejecutivo

El backend actual es un CRUD básico. Permite crear y consultar usuarios, reportes, ubicaciones y alertas, y protege las rutas con JWT. Sin embargo, todavía no cubre varios flujos que el informe presenta como parte del producto:

- El registro y el inicio de sesión no están unificados con el perfil.
- No existe recuperación real de contraseña.
- No se valida que un reporte, una ubicación o una alerta pertenezcan a un usuario o reporte existente.
- No hay cálculo de mapa de calor ni búsquedas por zona.
- No hay notificaciones, alertas por proximidad ni compartición de ubicación.
- No hay filtros, comentarios, verificación de reportes ni moderación.
- No existen los módulos de Notification ni Payment que aparecen en el diseño del informe.

En resumen: el backend cubre la base para almacenar datos, pero todavía no cubre la experiencia funcional que el informe promete.

---

## 2. Estado actual del backend

### 2.1. Lo que ya existe

| Módulo | Funcionalidad actual | Estado |
|---|---|---|
| Autenticación | Registro con usuario y contraseña, inicio de sesión, cambio de contraseña y emisión de JWT | Existe, pero con limitaciones |
| Usuarios | Crear perfil, obtener perfil por email, actualizar datos básicos y eliminar perfil | Parcial |
| Reportes | Crear, listar todos, obtener por ID, obtener por usuario y eliminar por ID | Parcial |
| Ubicaciones | Crear ubicación, listar todas y endpoint de “zonas peligrosas” | Parcial, el endpoint de zonas peligrosas no calcula nada |
| Alertas | Crear, listar todas, obtener por ID, obtener por usuario y eliminar todas | Parcial |
| Seguridad | JWT, BCrypt, roles básicos y rutas protegidas | Funciona, pero con fallos importantes |
| Documentación | Swagger/OpenAPI | Existe |

### 2.2. Endpoints actuales

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/v1/authentication/sign-up` | Registra credenciales de usuario |
| POST | `/api/v1/authentication/sign-in` | Inicia sesión y devuelve JWT |
| PUT | `/api/v1/authentication/change-password` | Cambia la contraseña |
| POST | `/api/v1/users` | Crea un perfil |
| GET | `/api/v1/users/{email}` | Busca un perfil por email |
| PUT | `/api/v1/users/{id}` | Actualiza nombre, apellido, teléfono e imagen |
| DELETE | `/api/v1/users/{id}` | Elimina perfil y usuario IAM |
| POST | `/api/v1/reports/` | Crea un reporte |
| GET | `/api/v1/reports/` | Lista reportes |
| GET | `/api/v1/reports/{id}` | Obtiene un reporte |
| GET | `/api/v1/reports/user/{userId}` | Lista reportes de un usuario |
| DELETE | `/api/v1/reports/{id}` | Elimina un reporte |
| POST | `/api/v1/locations/` | Crea una ubicación asociada a un reporte |
| GET | `/api/v1/locations/` | Lista ubicaciones |
| GET | `/api/v1/locations/dangerous` | Lista ubicaciones “peligrosas” |
| POST | `/api/v1/alerts/` | Crea una alerta |
| GET | `/api/v1/alerts/` | Lista alertas |
| GET | `/api/v1/alerts/{id}` | Obtiene una alerta |
| GET | `/api/v1/alerts/user/{userId}` | Lista alertas de un usuario |
| DELETE | `/api/v1/alerts/` | Elimina todas las alertas |

---

## 3. Funcionalidades del informe que faltan o están incompletas

### 3.1. Registro, inicio de sesión y cuenta

| Historia | Qué pide el informe | Qué hay hoy | Qué falta |
|---|---|---|---|
| US04 | Registro con validación de campos y control de correo duplicado | Se crea un usuario IAM y, por separado, un perfil | Unificar ambos registros, validar campos obligatorios y rechazar correos duplicados con un mensaje claro |
| US05 | Inicio de sesión con correo y contraseña | Se inicia sesión con `username`; el frontend usa el correo como username | Validar formato de correo, mantener el mismo identificador en todo el sistema y mostrar errores claros |
| US12 | Recuperar contraseña | Solo existe un cambio de contraseña que no verifica identidad | Flujo real de recuperación: solicitud, token temporal, correo o canal de recuperación, expiración y cambio seguro |
| TS01 | Login en `/api/v1/login` y error “Credenciales incorrectas” | Login en `/api/v1/authentication/sign-in`; el mensaje de error es genérico | Definir una ruta estable y mensajes de error consistentes |
| TS02 | Crear usuario desde `/api/v1/users` con estado 201 y correo único | `/api/v1/users` crea solo el perfil, devuelve 200 e ignora la contraseña | Que el alta de usuario cree identidad y perfil en un solo flujo, o documentar dos pasos claramente |
| TS03 | Editar perfil incluyendo correo | Se actualizan nombre, apellido, teléfono e imagen | Permitir actualizar el correo y validar que no quede duplicado |

Además, faltan funciones de cuenta que el producto necesita aunque el informe no las detalle:

- Cerrar sesión en todos los dispositivos.
- Ver y revocar sesiones activas.
- Verificación de correo.
- Cambio de contraseña con confirmación de la contraseña actual.
- Eliminación de cuenta con confirmación.
- Roles y permisos reales: hoy se puede pedir un rol desde el registro.

### 3.2. Perfil de usuario

| Historia | Qué pide el informe | Qué hay hoy | Qué falta |
|---|---|---|---|
| US11 | Editar información personal con validaciones y mensaje de confirmación | Se actualizan cuatro campos | Validar nombres, teléfono y correo; actualizar correo; devolver mensajes de error por campo |
| US14 | Ver perfil con imagen por defecto y opción de cambiarla | Se guarda una URL de imagen | Subida real de imagen o integración formal con almacenamiento; imagen por defecto controlada por el backend |
| TS13 | Obtener usuario por email y devolver 404 si no existe | Devuelve 400 con texto plano | Código 404, respuesta JSON uniforme y búsqueda segura |

Funciones de perfil que conviene añadir:

- Preferencias de idioma y tema.
- Radio de alertas.
- Contactos de confianza.
- Configuración de privacidad para compartir ubicación.
- Historial de reportes y alertas del usuario.
- Estado de verificación del usuario.
- Fecha de registro y última actividad.

### 3.3. Reportes de incidentes

| Historia | Qué pide el informe | Qué hay hoy | Qué falta |
|---|---|---|---|
| US06 | Crear reporte y actualizar el mapa de calor | Se guarda el reporte | Asociarlo correctamente a ubicación, usuario y mapa de calor; validar campos y evitar duplicados |
| US07 | Adjuntar fotos o videos como evidencia | Solo se guarda una URL de imagen | Subida de archivos, validación de tipo y tamaño, almacenamiento, miniaturas y varias evidencias por reporte |
| US08 | Ver reportes recientes y verlos en el mapa | Se listan todos los reportes | Orden por fecha, paginación, filtros por zona y fecha, y datos listos para pintar en el mapa |
| US15 | Filtrar entre “todos” y “mis reportes” | Existe consulta por usuario | Filtros combinables: usuario, tipo, fecha, zona, estado y texto |
| TS04 | Crear reporte con ubicación, descripción y tipo; error si faltan datos | Se crea con título, detalle, tipo, usuario, imagen y dirección | Validación obligatoria y mensaje funcional claro |
| TS05 | Listar reportes y responder 200 incluso sin datos | Lista vacía con 200 | Paginación y metadatos de respuesta |
| TS06 | Obtener reporte por ID y devolver 404 si no existe | Devuelve 400 | Código 404 y mensaje uniforme |

Funciones de reportes que faltan para que el flujo sea completo:

- Editar un reporte propio.
- Eliminar solo reportes propios, con reglas de autorización.
- Cambiar el estado del reporte: pendiente, en revisión, verificado, descartado o resuelto.
- Marcar severidad: baja, media, alta o crítica.
- Categorías controladas: robo, accidente, iluminación, acoso, emergencia, etc.
- Evitar reportes duplicados en la misma zona y tiempo.
- Confirmaciones o votos de otros usuarios.
- Comentarios en reportes.
- Reportar contenido abusivo.
- Historial de cambios.
- Fecha de expiración o vigencia del reporte.
- Contador de reportes por zona para alimentar el mapa de calor.

### 3.4. Ubicaciones y mapa

| Historia | Qué pide el informe | Qué hay hoy | Qué falta |
|---|---|---|---|
| TS07 | Crear ubicación asociada a un reporte existente | Se guarda una ubicación con `idReport` | Validar que el reporte exista y que la ubicación sea coherente |
| TS08 | Listar ubicaciones para pintar el mapa | Se listan todas | Consultas por zona, bounding box, radio o cercanía |
| US13 | Mapa con reportes en el área | El backend entrega listas completas | Endpoint de mapa que devuelva solo lo necesario para el área visible |
| US16 | Buscar una ubicación por dirección y mover el mapa | No hay búsqueda en backend | Geocodificación, búsqueda por dirección y consulta por coordenadas |
| US17 / mapa de calor | Zonas de mayor peligro | Existe `/locations/dangerous`, pero devuelve todas las ubicaciones y no usa el parámetro | Cálculo real de densidad, agrupación por zona y respuesta con niveles de riesgo |

Problemas funcionales actuales del módulo de ubicación:

- La ubicación se asocia a un reporte, no al usuario.
- No existe una “última ubicación del usuario”, aunque el informe habla de la ubicación del celular.
- No hay historial de ubicaciones.
- No hay relaciones de cercanía ni geocercas.
- No hay cálculo de mapa de calor.
- No hay rutas seguras.
- No hay clustering de marcadores.

Funciones de mapa recomendadas:

- Búsqueda por radio: reportes a 500 m, 1 km o 5 km.
- Mapa de calor por densidad de reportes.
- Agrupación de reportes por zona.
- Filtros por tipo, severidad y fecha.
- Capa de zonas seguras o rutas sugeridas.
- Historial de zonas consultadas.
- Cache o resumen por zona para no enviar todos los puntos al cliente.

### 3.5. Alertas y notificaciones

| Historia | Qué pide el informe | Qué hay hoy | Qué falta |
|---|---|---|---|
| US09 | Recibir una alerta al acercarse a una zona de riesgo | Se pueden crear alertas manualmente | Detección automática por proximidad, reglas de distancia y generación de alertas |
| TS09 | Crear alerta validando usuario y reporte | Se guarda sin validar existencia | Validar usuario, reporte y ubicación |
| TS10 | Obtener alertas por usuario | Existe consulta por usuario | Devolver 404 si el usuario no existe y separar “sin alertas” de “usuario inválido” |
| TS11 | Eliminar todas las alertas al recargar el mapa | Existe borrado global | Borrado por usuario o por sesión, no global |
| TS12 | Obtener alerta por ID y devolver 404 | Devuelve 400 | Código 404 y mensaje uniforme |

Funciones de alertas que faltan:

- Alertas automáticas por cercanía a un reporte.
- Notificaciones push al móvil.
- Notificaciones dentro de la web.
- Marcar alerta como leída o atendida.
- Historial de alertas.
- Silenciar alertas por tiempo o zona.
- Configurar radio de alerta por usuario.
- Evitar alertas duplicadas.
- Alertas para contactos de confianza.
- Alertas de emergencia enviadas por el usuario.

El informe también presenta un bounded context de Notification. Hoy no existe ningún módulo de notificaciones en el backend.

### 3.6. Compartir ubicación con contactos

La historia US10 pide compartir la ubicación en tiempo real con contactos cercanos. Hoy no existe:

- Modelo de contactos.
- Invitaciones o aceptación de contactos.
- Compartición temporal de ubicación.
- Permisos de quién puede ver la ubicación.
- Historial de ubicaciones compartidas.
- Detener la compartición.
- Notificación al contacto.

Para implementarlo haría falta, como mínimo:

- Relación usuario-contacto.
- Sesión de compartición con fecha de inicio y fin.
- Endpoint para actualizar la ubicación durante la sesión.
- Endpoint para consultar la última ubicación compartida.
- Reglas de privacidad y consentimiento.

### 3.7. Búsqueda y filtros

El informe menciona filtros y búsqueda en varias historias, pero el backend actual solo tiene “reportes por usuario” y “alertas por usuario”.

Faltan filtros por:

- Tipo de incidente.
- Severidad.
- Fecha o rango de fechas.
- Zona, radio o coordenadas.
- Estado del reporte.
- Usuario que reportó.
- Texto en título, detalle o dirección.
- Reportes verificados o no verificados.

También falta paginación y ordenamiento. Hoy todos los listados devuelven todo. Eso funciona con pocos datos, pero no con un mapa real.

### 3.8. Comunidad y moderación

El informe no desarrolla a fondo la moderación, pero el producto la necesita si va a recibir reportes ciudadanos. Faltan:

- Comentarios en reportes.
- Confirmaciones de otros usuarios.
- Verificación comunitaria.
- Reporte de contenido falso o abusivo.
- Panel de moderación.
- Roles de administrador y moderador con permisos reales.
- Historial de acciones de moderación.
- Bloqueo o suspensión de usuarios.
- Reglas para reportes duplicados.

### 3.9. Módulos del diseño que no existen

El informe muestra estos bounded contexts:

- Authentication.
- Notification.
- Reports.
- Location.
- Payment.

En el backend actual existen Authentication, Reports, Location y parte de usuarios, pero no existen:

- Notification: no hay envío de notificaciones, preferencias ni historial.
- Payment: no hay planes, suscripciones, pagos ni límites por plan.
- Usuarios como contexto separado: el perfil está mezclado con la identidad y no hay un modelo de usuario completo.

Si los planes o pagos no van a formar parte del alcance real, conviene quitarlos del informe o marcarlos como fuera de alcance. Si sí van a existir, falta todo el flujo funcional: planes, suscripción, facturación, límites y estado de cuenta.

### 3.10. Funcionalidades “to-be” del informe

El capítulo 8 del informe propone experimentos futuros. Su relación con el backend es esta:

| Historia | Funcionalidad | Dependencia del backend |
|---|---|---|
| UA01 | Modo oscuro | Principalmente frontend; el backend solo guardaría la preferencia |
| UA02 | Filtros por tipo de reporte | Requiere filtros y parámetros en reportes |
| UA03 | Comentarios y verificación de reportes | Requiere modelo de comentarios, votos y estado de verificación |
| UA04 | Cambio de idioma | Principalmente frontend; el backend guardaría el idioma preferido |
| UA05 | Ajuste de tamaño del mapa | Principalmente frontend; el backend debe soportar consultas por área |

---

## 4. Funcionalidades que no son del backend

Estas historias del informe son principalmente de frontend, landing page o aplicación móvil. Se listan para separar responsabilidades:

- US01: formulario de contacto de la landing page.
- US02: navegación de la landing page.
- US03: diseño responsivo.
- Partes visuales de US13, US14 y US16.
- Modo oscuro, cambio de idioma y tamaño del mapa.

Si el formulario de contacto debe guardar mensajes o enviarlos desde el backend, entonces sí haría falta un endpoint y un almacenamiento de mensajes. Hoy no existe.

---

## 5. Mejoras funcionales recomendadas más allá del informe

Estas mejoras no están detalladas en el informe, pero tienen sentido para el contexto de una app de seguridad ciudadana.

### 5.1. Reportes más confiables

- Estados del reporte: pendiente, en revisión, verificado, descartado y resuelto.
- Nivel de severidad.
- Categorías cerradas y consistentes.
- Evidencia obligatoria para ciertos tipos.
- Fecha y hora del incidente, no solo de creación.
- Confirmación de otros usuarios.
- Comentarios.
- Historial de cambios.
- Reglas anti-duplicados.
- Reportes anónimos, si el producto lo permite.
- Tiempo de vigencia del reporte.

### 5.2. Mapa útil, no solo un listado

- Mapa de calor real por densidad.
- Consultas por área visible.
- Agrupación de reportes.
- Búsqueda por dirección.
- Filtros por tipo, fecha, severidad y estado.
- Zonas seguras y rutas sugeridas.
- Reportes cercanos a la ubicación del usuario.
- Cache de resumen por zona.
- Actualización en tiempo real o casi real.

### 5.3. Alertas que realmente ayuden

- Detección de proximidad con radio configurable.
- Notificaciones push.
- Alertas leídas y no leídas.
- Historial.
- Silenciar por zona o por tiempo.
- Alertas a contactos de confianza.
- Botón de emergencia.
- Prevención de duplicados.
- Alertas basadas en severidad y tipo.

### 5.4. Cuenta y privacidad

- Verificación de correo.
- Recuperación de contraseña.
- Sesiones activas.
- Cierre de sesión global.
- Preferencias de notificaciones.
- Privacidad de ubicación.
- Consentimiento para compartir ubicación.
- Eliminación de cuenta.
- Roles y permisos reales.

### 5.5. Comunidad y confianza

- Comentarios.
- Votos de confirmación.
- Reportes verificados.
- Reputación del usuario.
- Moderación.
- Reporte de abuso.
- Historial de sanciones.
- Panel de administración.

### 5.6. Analítica funcional

- Reportes por zona.
- Reportes por tipo.
- Reportes por fecha.
- Zonas con más incidentes.
- Tiempo promedio de atención.
- Usuarios activos.
- Alertas generadas.
- Reportes verificados vs. descartados.

---

## 6. Plan de mejora funcional sugerido

### Fase 1: cerrar el flujo de cuenta

Objetivo: que un usuario pueda registrarse, iniciar sesión, editar su perfil y recuperar su contraseña sin inconsistencias.

- Unificar identidad y perfil.
- Validar correo único.
- Validar campos obligatorios.
- Implementar recuperación de contraseña.
- Corregir cambio de contraseña.
- Permitir actualizar correo.
- Devolver códigos y mensajes coherentes.

### Fase 2: reportes y mapa

Objetivo: que un reporte se cree completo, se vea en el mapa y se pueda consultar por zona.

- Validar ubicación y usuario.
- Asociar reporte y ubicación de forma consistente.
- Implementar filtros y paginación.
- Implementar consultas por área.
- Implementar mapa de calor.
- Implementar edición y borrado con permisos.
- Agregar estados, severidad y categorías.

### Fase 3: alertas y notificaciones

Objetivo: que el usuario reciba avisos útiles cuando esté cerca de una zona de riesgo.

- Detección por proximidad.
- Alertas por usuario.
- Notificaciones push.
- Marcar como leída.
- Historial.
- Configuración de radio.
- Evitar duplicados.

### Fase 4: comunidad y confianza

Objetivo: que los reportes sean más confiables y que la comunidad participe.

Estado: implementada (ver `README.md`, sección "API por fases"). Quedan fuera la suspensión de usuarios y la reputación, que se trasladan a la Fase 5 junto con los roles.

- Comentarios.
- Confirmaciones.
- Verificación.
- Moderación.
- Reporte de abuso.
- Panel de administración.
- Métricas de calidad.

### Fase 5: funciones avanzadas

Objetivo: completar la visión del producto.

- Compartir ubicación con contactos.
- Contactos de confianza.
- Rutas seguras.
- Planes o suscripciones, si se mantienen en el alcance.
- Analítica y exportación.
- Preferencias de idioma y tema.

---

## 7. Criterios de aceptación funcionales clave

Estos criterios pueden usarse para cerrar cada módulo.

### Registro

- No se permite registrar dos veces el mismo correo.
- Se muestran los campos faltantes.
- El usuario creado puede iniciar sesión.
- El perfil queda asociado al usuario correcto.

### Inicio de sesión

- Credenciales correctas devuelven token.
- Credenciales incorrectas devuelven error claro.
- Un usuario inactivo o bloqueado no puede entrar.
- El token identifica al usuario en todas las operaciones.

### Perfil

- Se puede consultar el perfil propio.
- Se puede editar nombre, apellido, teléfono, imagen y correo.
- El correo editado sigue siendo único.
- Los errores se muestran por campo.

### Reportes

- Un reporte requiere usuario, tipo, descripción y ubicación.
- Un reporte solo puede ser editado o eliminado por su dueño o un moderador.
- Un reporte aparece en el mapa después de crearse.
- Los reportes se pueden filtrar y paginar.
- Un reporte inexistente devuelve 404.

### Ubicaciones y mapa

- Una ubicación solo se crea si el reporte existe.
- El mapa solo recibe los puntos del área consultada.
- El mapa de calor refleja la densidad de reportes.
- Se puede buscar por dirección o por coordenadas.

### Alertas

- Solo se crea una alerta para un usuario y reporte válidos.
- No se duplican alertas para el mismo evento.
- El usuario puede ver sus alertas.
- El usuario puede marcar una alerta como leída.
- Las alertas se pueden silenciar o configurar.

### Notificaciones

- El usuario recibe la notificación en el canal configurado.
- Puede ver el historial.
- Puede desactivar tipos de notificación.
- Las notificaciones no se duplican.

---

## 8. Decisiones que hay que tomar antes de implementar

1. ¿El identificador principal del usuario será el correo, el username o el ID interno?
2. ¿El perfil y la identidad serán una sola entidad o dos entidades separadas?
3. ¿La ubicación pertenece al usuario, al reporte o a ambos?
4. ¿El mapa de calor se calcula en backend o en frontend?
5. ¿Las alertas se generan en backend, en el móvil o en ambos?
6. ¿Las notificaciones serán push, correo, dentro de la app o varias?
7. ¿Los reportes serán anónimos o siempre tendrán usuario?
8. ¿Quién puede moderar y qué permisos tendrá?
9. ¿Los planes y pagos están dentro del alcance real?
10. ¿Se guardarán archivos en el backend o en un servicio externo?

---

## 9. Observaciones del informe que conviene corregir

- El login aparece como `/api/v1/login`, pero el backend usa `/api/v1/authentication/sign-in`.
- La tabla de endpoints dice 201 para crear alertas y ubicaciones, mientras los criterios de aceptación dicen 200.
- El ID `US03` se usa tanto para “Diseño responsivo” como para “Acceso a planes” en el backlog.
- Los criterios de aceptación de US12 parecen copiados de la edición de perfil y no describen una recuperación de contraseña.
- El informe muestra contextos de Notification y Payment que no existen en el backend.
- El informe presenta el mapa de calor como una funcionalidad principal, pero el endpoint de zonas peligrosas no calcula densidad ni filtra resultados.

---

## 10. Conclusión

El backend actual sirve como base para almacenar y consultar información, pero todavía no implementa la mayor parte de la experiencia funcional que el informe describe. El siguiente paso no debería ser agregar más endpoints aislados, sino cerrar flujos completos:

1. Cuenta y perfil.
2. Reporte y ubicación.
3. Mapa y búsqueda.
4. Alertas y notificaciones.
5. Comunidad y moderación.

Si se completan esas cinco líneas, el backend dejaría de ser un CRUD básico y se acercaría a lo que el producto promete en el informe.

# EFT Manager

Aplicación web para gestionar el progreso personal en *Escape from Tarkov*: refugio (hideout), mercado de ítems, misiones (tasks), favoritos y comparación de progreso con otros jugadores. Los datos del juego (ítems, estaciones, traders, misiones, mapas, precios) se obtienen en tiempo real de la API pública de [tarkov.dev](https://tarkov.dev), mientras que el progreso del usuario se gestiona y persiste en la propia aplicación.

## Índice

- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Requisitos previos](#requisitos-previos)
- [Puesta en marcha](#puesta-en-marcha)
- [Variables de entorno](#variables-de-entorno)
- [Tests y cobertura](#tests-y-cobertura)
- [Documentación de la API](#documentación-de-la-api)
- [Funcionalidades](#funcionalidades)
- [Roles y permisos](#roles-y-permisos)
- [Limitaciones conocidas](#limitaciones-conocidas)
- [Despliegue](#despliegue)

## Stack tecnológico

**Backend**
- Java 21+ / Spring Boot 4
- Spring Security + JWT
- Spring Data JPA + PostgreSQL
- Flyway (migraciones de base de datos)
- Springdoc OpenAPI (documentación Swagger)
- Testcontainers + JUnit 5 + Mockito (tests)
- JaCoCo (cobertura de tests)

**Frontend**
- React + Vite
- React Router (enrutado y persistencia de filtros vía query params)
- Recharts (gráficas del histórico de precios)
- Context API (autenticación, modo de juego, idioma, favoritos, notificaciones)

**Infraestructura**
- PostgreSQL vía Docker
- API externa: [json.tarkov.dev](https://json.tarkov.dev)

## Arquitectura

El backend está organizado **por feature**, no por capa técnica — cada carpeta bajo `cat.itacademy.s05.t02.eftmanager` agrupa todo lo relativo a un dominio concreto (controller, service, DTOs, entidades y excepciones propias):

```
auth/          user/          hideout/       item/
skill/         category/      trader/        price/
barter/        task/          map/           favorite/
admin/         dashboard/     security/      config/
common/        (GameMode, CurrentUserResolver, TarkovMetadataService,
                excepciones transversales)
```

Los servicios que consumen la API externa de Tarkov.dev siguen un patrón consistente en todo el proyecto:
- Catálogo y traducciones cacheados en memoria (`@Cacheable`), con refresco automático cada 24h.
- Auto-inyección con `@Lazy` (`private final XxxService self`) para que la caché siga funcionando correctamente incluso en llamadas internas entre métodos del mismo servicio (limitación conocida de los proxies de Spring).
- Resolución de nombres traducidos mediante un diccionario clave→texto separado del catálogo de datos, con *fallback* al nombre normalizado en inglés si la traducción no existe.

El frontend sigue una estructura equivalente por página/dominio, con contextos globales (`AuthContext`, `GameModeContext`, `LanguageContext`, `FavoritesContext`, `ToastContext`) para el estado compartido entre vistas.

## Requisitos previos

- JDK 21+
- Docker Desktop (para la base de datos en desarrollo y para los tests de integración con Testcontainers)
- Node.js 18+ y npm
- Una IDE con soporte Maven integrado (recomendado: IntelliJ IDEA — el proyecto no requiere `mvn` instalado globalmente en el sistema)

## Puesta en marcha

### 1. Clona ambos repositorios (backend y frontend)

### 2. Backend

```bash
# Levanta PostgreSQL
docker compose up -d

# Copia el archivo de ejemplo y rellena tus propios valores
cp .env.example .env
```

Arranca la aplicación desde tu IDE (o `Run` sobre `EftmanagerApplication`). Flyway aplicará automáticamente todas las migraciones al arrancar.

### 3. Frontend

```bash
cd eftmanager-frontend
npm install
cp .env.example .env   # ajusta VITE_API_URL si es necesario
npm run dev
```

La aplicación quedará disponible en `http://localhost:3000` (frontend) y `http://localhost:8080` (backend).

## Variables de entorno

### Backend (`.env`)

| Variable | Descripción | Ejemplo |
|---|---|---|
| `DB_NAME` | Nombre de la base de datos | `eftmanager` |
| `DB_USERNAME` | Usuario de PostgreSQL | `postgres` |
| `DB_PASSWORD` | Contraseña de PostgreSQL | `postgres` |
| `JWT_SECRET` | Clave secreta para firmar los JWT (mínimo 256 bits, en Base64) | *generar con* `openssl rand -base64 32` |
| `JWT_EXPIRATION_MS` | Duración del token en milisegundos | `3600000` |
| `TARKOV_API_URL` | URL base de la API pública de Tarkov.dev | `https://json.tarkov.dev` |
| `TARKOV_API_LANGUAGE` | Idioma por defecto para peticiones sin `lang` explícito | `es` |

### Frontend (`.env`)

| Variable | Descripción | Ejemplo |
|---|---|---|
| `VITE_API_URL` | URL base del backend | `http://localhost:8080/api` |

## Tests y cobertura

Los tests se ejecutan a través del ciclo de vida de Maven (fase `test`), no ejecutando clases sueltas — esto es necesario para que el plugin de JaCoCo se dispare correctamente.

**Desde IntelliJ:** panel de Maven → `Lifecycle` → doble clic en `test`.

**Con Docker abierto**, ya que los tests de integración levantan un contenedor real de PostgreSQL vía Testcontainers (no se usa ninguna base de datos simulada).

El informe de cobertura se genera en:
```
target/site/jacoco/index.html
```

**Cobertura actual: ~65%** sobre el umbral mínimo del 60% configurado en el `pom.xml` (`jacoco-maven-plugin`, regla `LINE COVEREDRATIO`).

### Tipos de test incluidos

- **Unitarios** (Mockito): lógica de negocio aislada — validación de niveles del hideout, cálculo de requisitos y comparación de progreso de misiones, resolución de favoritos, gestión de roles de administrador, parseo de catálogos externos.
- **De aceptación** (MockMvc + Testcontainers): flujos HTTP completos contra el contexto real de Spring — registro, login, acceso a endpoints protegidos/públicos, ciclo completo de favoritos, manejo global de excepciones.
- **De regresión de seguridad**: verifican explícitamente que un JWT roto o manipulado nunca produce un error 500, sino un 403 controlado.

### Nota sobre dos tests marcados como `@Disabled`

`FavoriteControllerIntegrationTest` y `GlobalExceptionHandlerIntegrationTest` están desactivados con un motivo documentado en el propio código. **No es un fallo de lógica**: ambos pasan correctamente en ejecución aislada, pero fallan de forma intermitente al ejecutarse dentro de una tanda larga de tests de integración, por presión de recursos de Docker Desktop sobre WSL2 en Windows (el contenedor de PostgreSQL deja de responder momentáneamente al encadenar varios contextos Spring completos). Se documenta la causa en vez de ocultar el problema.

## Documentación de la API

Con el backend en marcha:
```
http://localhost:8080/swagger-ui/index.html
```

## Funcionalidades

- **Autenticación**: registro, login con JWT, cambio de contraseña.
- **Refugio (Hideout)**: progreso por estación y modo de juego, requisitos de mejora (ítems, traders, otras estaciones, skills), resumen agregado de materiales pendientes, distintas distribuciones visuales configurables.
- **Mercado de ítems**: catálogo completo con buscador, filtro por categoría (árbol jerárquico), ordenación, favoritos por modo de juego, histórico de precios con selector de rango temporal, opciones de compra/venta a traders, trueques.
- **Misiones (Tasks)**: catálogo completo con filtros (trader, mapa, estado, Kappa, Lightkeeper), detalle enriquecido (objetivos, recompensas, requisitos previos con indicador de bloqueo), notificaciones de desbloqueo tras completar una misión con tiempo de espera.
- **Comparación de progreso**: comparación de misiones completadas con cualquier otro usuario registrado, desglosada por trader, sin necesidad de un sistema de amistades previo.
- **Dashboard**: resumen de progreso del hideout, misiones y favoritos más valiosos, por modo de juego.
- **Internacionalización**: selector de idioma (ES/EN) con detección automática inicial.
- **Panel de administración**: gestión de usuarios (listado, cambio de rol, eliminación), protegido por rol tanto en frontend como en backend.

## Roles y permisos

| Rol | Permisos |
|---|---|
| `USER` | Acceso completo a hideout, mercado, misiones, favoritos, comparación y dashboard propios |
| `ADMIN` | Todo lo anterior + panel de administración (`/api/admin/**`) |


# Tenis Ahora

Sistema de gestión y reserva de canchas de tenis para un club. Proyecto final de Ingeniería de Software.

**Materia:** Ingeniería de Software · **Profesor:** Ing. Juan Piñeiro

Permite a los socios reservar canchas (con alquiler de insumos), y al personal del club administrar
usuarios, canchas, stock, reservas y cobros.

## Contenido

- [Estructura del repositorio](#estructura-del-repositorio)
- [Tecnologías](#tecnologías)
- [Cómo levantarlo en local](#cómo-levantarlo-en-local)
- [Usuarios de prueba](#usuarios-de-prueba)
- [Roles y permisos](#roles-y-permisos)
- [Estado del proyecto](#estado-del-proyecto)
- [Arquitectura del backend](#arquitectura-del-backend)
- [Configuración](#configuración)
- [Tests](#tests)
- [Equipo](#equipo)

## Estructura del repositorio

| Carpeta | Qué es |
| --- | --- |
| [`backend/`](backend) | API REST (Java + Spring Boot) |
| [`backend/docs/`](backend/docs) | Documentación técnica: DER, decisiones, y el contrato de la API de reservas y cobros; arquitectura en preparación |
| [`front/`](front) | Frontend (React + Vite) |

## Tecnologías

| Parte | Stack |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5, Spring Security con JWT, Spring Data JPA, PostgreSQL 16, Swagger (springdoc), OpenPDF, Lombok, Maven |
| Frontend | React 19, Vite, Tailwind CSS 4, shadcn/ui, React Router 7, Zustand, Axios, Vitest + Testing Library, pnpm |
| Infraestructura | Docker Compose para la base local. El hosting de producción todavía no está decidido |
| Integraciones | Mercado Pago (SDK oficial: débito/crédito y QR vía Checkout Pro, para el cobro de la seña) ya implementado. Email saliente, previsto |

## Cómo levantarlo en local

Requisitos: Java 21, Maven 3.9+ (el repo no incluye `mvnw`), Node.js 20+, pnpm y Docker.

### 1. Backend

```bash
cd backend
cp .env.example .env     # opcional en desarrollo: el perfil dev trae valores por defecto
docker compose up -d     # PostgreSQL en el puerto 5432
mvn spring-boot:run      # API en http://localhost:8080
```

- Si ya tenés PostgreSQL en el puerto 5432, mirá el comentario al principio de `backend/docker-compose.yml`
  para usar el 5433.
- En el perfil `dev` el esquema se crea y actualiza solo (`ddl-auto: update`) y un `DataSeeder`
  carga usuarios, canchas, horarios e insumos de prueba. El seeder **no corre en producción**.
- La documentación interactiva de la API está en `http://localhost:8080/swagger-ui`.
- En VS Code hay una configuración de lanzamiento ("Tenis Ahora (dev)") en `backend/.vscode`.

### 2. Frontend

```bash
cd front
cp .env.example .env.development   # VITE_API_URL=http://localhost:8080/api
pnpm install
pnpm dev                           # http://localhost:5173
```

## Usuarios de prueba

Los crea el `DataSeeder` en desarrollo. La contraseña de los tres es `TenisAhora2026!`.

| Rol | Email |
| --- | --- |
| Administrador | `admin@tenisahora.com` |
| Empleado | `empleado@tenisahora.com` |
| Socio | `socio@tenisahora.com` |

## Roles y permisos

| Rol | Qué puede hacer |
| --- | --- |
| `SOCIO` | Ver la disponibilidad de canchas, reservar, ver y cancelar sus reservas, asignar insumos a su reserva |
| `EMPLEADO` | Todo lo del socio, más: gestionar socios, canchas (horarios y bloqueos), insumos y stock, buscar y modificar reservas, iniciar y finalizar alquileres |
| `ADMINISTRADOR` | Todo lo del empleado, más la gestión de administradores |

Los permisos por ruta están en `backend/src/main/java/com/tenisahora/config/SecurityConfig.java`.
En el modelo también existen `PROFESOR` y `ENTRENADOR` (módulo `instructor`), que el frontend ya
contempla en sus rutas protegidas.

## Estado del proyecto

Los casos de uso (CU) siguen la numeración de la documentación de la cátedra.

| Módulo | Backend | Frontend |
| --- | --- | --- |
| Autenticación (login, registro, refresh con cookie, bloqueo por intentos) | Listo | Login y registro |
| Usuarios: socios, empleados, administradores (CU01–CU05) | Listo | Pantalla de gestión de usuarios |
| Canchas, horarios y disponibilidad | Listo (`/api/canchas`) | Rama en desarrollo |
| Insumos y stock (CU10) | Listo (`/api/insumos`) | Rama en desarrollo |
| Reservas: registrar, consultar, modificar, cancelar, insumos, inicio y fin del alquiler (CU13–CU19) | Listo (`/api/reservas`) | Pendiente |
| Cobros y recibos: seña del 50 % con débito, crédito o QR de Mercado Pago, webhook y conciliación (CU51, CU57, CU77, CU78, CU86–CU88) | Listo (`/api/cobros`, `/api/reservas/{id}/cobros`, `/webhooks/mercadopago`) | Pendiente |
| Descuentos (RD21) | El cálculo se aplica en el cobro; el modelo de elegibilidad por socio (liga, escuela, paquete de horas) es otro ticket | Rama en desarrollo |
| Profesores y entrenadores (instructores) | Solo entidades y servicios | Rama en desarrollo |
| Clases, entrenamientos y asistencia: programar, buscar, modificar, eliminar, imprimir ficha, inscripción de socios, "mis clases"/"mis entrenamientos", registro de asistencia y asignación de instructor a actividad particular (CU32–CU36, CU37–CU44, CU70, CU80–CU83) | Listo (`/api/clases`, `/api/entrenamientos`, `/api/actividades`) | Pendiente |
| Torneos | Solo entidades | Pendiente |
| Reportes, permisos y club | Sin implementar | Pendiente |

Tareas automáticas ya implementadas: cancelación de reservas pendientes de pago vencidas,
finalización de alquileres en curso vencidos, vencimiento de QR de Mercado Pago sin pagar,
conciliación de pagos cuyo aviso se perdió, purga de refresh tokens y limpieza de límites por IP.

## Arquitectura del backend

Estructura por módulos, cada uno con sus capas adentro (decisión
[001](backend/docs/decisiones/001-estructura-por-modulos.md)):

```
com.tenisahora
├── modules/            un paquete por módulo
│   └── <modulo>/       controller · service · repository · entity · dto · mapper · job
├── shared/             código común, no conoce a ningún módulo
├── infrastructure/     email, Mercado Pago, PDF
├── config/             seguridad, CORS, Swagger, reloj, tareas programadas, DataSeeder
├── middleware/         filtro JWT y límite de pedidos por IP
└── security/
```

Los módulos son: `actividad`, `administrador`, `auth`, `cancha`, `club`, `cobro`, `descuento`,
`empleado`, `instructor`, `insumo`, `permiso`, `reporte`, `reserva`, `socio`, `torneo`.

Reglas que se verifican con tests de arquitectura (`ArquitecturaTest`): sin ciclos entre módulos,
`security`/`shared`/`infrastructure` no dependen de ningún módulo, y `cancha`/`insumo` solo exponen
puertos (otros módulos no les importan el repository). **Pendiente de agregar:** la regla general de
"un módulo usa el service de otro, nunca su repository" todavía no tiene su propio test — hoy
`reserva` la viola en 4 imports (`CanchaRepository`/`SocioRepository` directo). Queda para el
saneamiento de arquitectura después de BACK 08 (rama aparte).

En [`backend/docs`](backend/docs) está el [DER](backend/docs/base-de-datos/der.md), la decisión de
estructura por módulos, y el [contrato de la API](backend/docs/api/contrato.md) de reservas y
cobros. Arquitectura y el diccionario de datos, por ahora tienen solo un `LEEME.md`.

## Configuración

Variables de entorno del backend (ver `backend/.env.example`):

| Variable | Para qué |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` (por defecto) o `prod` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Conexión a PostgreSQL. Contra una base gestionada la URL suele terminar en `?sslmode=require` |
| `JWT_SECRET` | Clave para firmar los JWT: mínimo 32 caracteres, distinta en producción |
| `FRONTEND_URL` | Orígenes del frontend permitidos por CORS, separados por coma |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | Email saliente (vacío si no se usa) |
| `MP_ACCESS_TOKEN` | Credencial de la cuenta vendedora DE PRUEBA de Mercado Pago |
| `MP_WEBHOOK_SECRET` | Clave para verificar la firma del webhook de pagos (`POST /webhooks/mercadopago`) |
| `COOKIE_SECURE`, `COOKIE_SAME_SITE` | Cookie del refresh token. En producción: `secure=true` |
| `SWAGGER_ENABLED` | En producción se puede apagar con `false` |

Las reglas de negocio de las reservas se ajustan en `backend/src/main/resources/application.yml`
(sección `app.reservas`): antelación máxima de 30 días, duración máxima de 2 horas, plazo de
cancelación de 6 horas, seña del 50 %, entre otras.

Variable del frontend: `VITE_API_URL`, la URL base de la API. En producción se carga como variable
de entorno del deploy.

El `.env` **nunca se sube a Git**.

## Tests

```bash
# Backend
cd backend && mvn test

# Frontend
cd front
pnpm test        # una corrida
pnpm test:watch  # modo observador
pnpm lint
pnpm build
```

## Equipo

- Micaela Dominguez
- Franco Herrera
- Lautaro Aguirre
- Victor Balbuena
- Maximiliano Orellana

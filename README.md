# Apunta API

API REST para la gestión de finanzas personales: registro de gastos, organización por categorías y control de presupuestos con alertas de sobregiro. Construida con Spring Boot 4, autenticación JWT y verificación de email asíncrona.

---

## Tabla de contenidos

- [Sobre el proyecto](#sobre-el-proyecto)
- [Características](#características)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura](#arquitectura)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Como probarlo](#como-probarlo)
- [Variables de entorno](#variables-de-entorno)
- [Endpoints](#endpoints)
- [Despliegue](#despliegue)
- [Roadmap](#roadmap)
- [Autor](#autor)

---

## Sobre el proyecto

**Apunta** es un proyecto personal para resolver un problema concreto: control real de los gastos.

Su versión 1.0 ya está lista.

**Objetivos:**
1. Reforzar mis conocimientos sobre programación en Java.
2. Construir una solución a un problema real, para que yo y cualquiera pueda usar.
3. Reto personal para aplicar buenas prácticas en el desarrollo de APIs con Spring Boot moderno.

---
## Características

- **Autenticación con JWT** stateless y verificación de email obligatoria antes del primer login.
- **Envío asíncrono de emails** vía Resend.
- **Gestión de gastos** con filtros, paginación y estadísticas (totales por usuario, por categoría).
- **Categorías personalizadas por usuario** con soft delete y reactivación.
- **Presupuestos** para gastos de una sola categoría o para cualquier tipo de gasto.
- **Estadísticas de usuario**: total gastado, distribución por categoría.
- **Documentación interactiva** con Swagger.
- **CI/CD automatizado**: tests en cada push a `main` y deploy automático a EC2 vía SSH.
- **Manejo global de errores** con respuestas estructuradas y códigos HTTP correctos.

---

## Stack tecnológico

- **Lenguaje:** Java 25
- **Framework:** Spring Boot 4.0.4
- **Seguridad:** Spring Security + JWT (jjwt 0.13.0) + BCrypt
- **Persistencia:** Spring Data JPA + Hibernate
- **Migraciones:** Flyway (las corre automáticamente al arrancar la app)
- **Base de datos:** MySQL 8.4 (AWS RDS)
- **Email:** Resend
- **Documentación:** Swagger
- **Testing:** JUnit 5, Spring Boot Test, H2 (in-memory)
- **Build:** Maven
- **Contenedores:** Docker
- **CI/CD:** GitHub Actions
- **Hosting:** AWS EC2
---

## Arquitectura

El proyecto sigue una **arquitectura modular por features** (no por capas técnicas). Cada feature agrupa su propio controller, service, repository, entidad, mapper y DTOs.

---

## Estructura del proyecto

```
src/main/java/pe/com/carlosh/tallyapi/
├── auth/                  # Registro, login, verificación de email
├── user/                  # Perfil, onboarding, estadísticas
├── category/              # CRUD de categorías personalizadas
├── budget/                # Presupuestos (con categoría y sin categoría)
├── expense/               # CRUD de gastos + agregaciones
├── notification/          # Servicio de envío de emails (Resend)
├── security/              # SecurityConfig, JwtService, filtros
└── core/exception/        # GlobalExceptionHandler y excepciones personalizadas
```

---

## Como probarlo

### Requisitos previos

- [Docker](https://www.docker.com)
- Cuenta en [Resend](https://resend.com) para obtener una API key (**opcional**, solo si quieres recibir el correo real de verificación; si no la configuras, el enlace de verificación se imprime en los logs del contenedor)

> No necesitas instalar Java ni Maven. Todo corre dentro de Docker.

### Pasos


### 1. Clonar el repo
```bash
git clone https://github.com/carloshuarcayah/apunta-api
cd apunta-api
```

### 2. Configura las variables de entorno

```bash
cp .env.example .env
```

Luego edita el archivo `.env` con tus valores. Los campos marcados como obligatorios deben completarse:

```bash
# Obligatorios
DATASOURCE_PASSWORD=tu_password_de_base_de_datos
JWT_SECRET=una_cadena_aleatoria_de_al_menos_32_caracteres

# Si quieres probar el envío de emails
RESEND_API_KEY=re_tu_api_key
RESEND_FROM_EMAIL=onboarding@resend.test
FRONTEND_VERIFICATION_URL=http://localhost:5173/verify

# Opcionales (tienen valores por defecto)
DATASOURCE_URL=jdbc:mysql://localhost:3306/tally_db
MYSQL_DATABASE=tally_db
DATASOURCE_USERNAME=tally_user
JWT_EXPIRATION=86400000
```

### 3. Levanta todo con Docker Compose

```bash
docker compose up
```

La primera vez tardará unos minutos mientras descarga las imágenes y construye la API. Cuando veas `Started TallyApiApplication`, ya está listo.

### 4. Para probar los endpoints

La documentación interactiva en `http://localhost:8080/swagger-ui.html`.

### 5. Verificar que la app está saludable

```bash
curl http://localhost:8080/actuator/health
```

Debería devolver `{"status":"UP"}`. Si ves `DOWN` o no responde, revisa los logs con `docker logs <nombre_container>`.

---

## Variables de entorno

| Variable              | Descripción                                   | Default                               |
|-----------------------|-----------------------------------------------|---------------------------------------|
| `DATASOURCE_URL`      | JDBC URL de MySQL                             | `jdbc:mysql://localhost:3306/tally_db`|
| `MYSQL_DATABASE`      | Nombre de la BD                               | `tally_db`                            |
| `DATASOURCE_USERNAME` | Usuario de la BD                              | `tally_user`                          |
| `DATASOURCE_PASSWORD` | Contraseña de la BD                           | — (requerido)                         |
| `JWT_SECRET`          | Clave HMAC para firmar tokens                 | — (requerido)                         |
| `JWT_EXPIRATION`      | TTL del JWT en milisegundos                   | `86400000` (24h)                      |
| `RESEND_API_KEY`      | API key de Resend                             | `re_xxxxxxxxx`                        |
| `RESEND_FROM_EMAIL`   | Remitente verificado                          | `onboarding@resend.test`              |
| `FRONTEND_VERIFICATION_URL` | URL del frontend para verificar cuenta por email | `http://localhost:5173/verify` |
| `HIBERNATE_DDL`       | Estrategia DDL de Hibernate (con Flyway en uso)| `validate`                            |
| `SHOW_SQL`            | Loggear SQL en consola                        | `true`                                |
| `FORMAT_SQL`          | Formatear el SQL loggeado                     | `true`                                |

---

## Endpoints
Para la documentación completa e interactiva, abre Swagger UI en tu navegador
una vez que tengas la app corriendo:

La documentación interactiva en `http://localhost:8080/swagger-ui.html`.

### Ejemplo: flujo completo de registro e inicio de sesión

**1. Registrar un nuevo usuario**

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "tu_cuenta@example.com",
    "username": "unnombre",
    "password1": "miPassword123",
    "password2": "miPassword123",
    "firstName": "Prueba1",
    "lastName": "Prueba1",
    "phone": "+51999888777"
  }'
```

**Respuesta:**
```json
{
  "message": "Registro exitoso. Por favor revisa tu bandeja de entrada para verificar tu cuenta."
}
```

**2. Verificar el email**

Al registrarte recibirás un correo con un enlace de verificación. El enlace apunta al **frontend** (la URL configurada en `FRONTEND_VERIFICATION_URL`, por defecto `http://localhost:5173/verify?token=xxx`), que luego llama al backend en `GET /api/auth/verify?token=xxx`.

Tienes dos formas de completar este paso:

**Opción A: Desde el correo (necesitas el frontend corriendo, no incluido en este repo)**

Haz clic en el botón "Verificar Cuenta" del email o copia el enlace en tu navegador. El frontend se encarga de llamar al backend.

**Opción B: Llamar al backend directamente (sin frontend)**

Copia el token que aparece al final del enlace del correo (después de `?token=`) y úsalo así:

```bash
curl -X GET "http://localhost:8080/api/auth/verify?token=TU_TOKEN_AQUI"
```

> Si **no** configuraste `RESEND_API_KEY`, el correo no se enviará pero el token sí se genera. Búscalo en los logs del contenedor:
> ```bash
> docker logs <nombre_contenedor_api> | grep "Verification link"
> ```

**Respuesta:**
```json
{
  "message": "Correo verificado exitosamente. Ya puedes volver a la aplicación e iniciar sesión."
}
```

> El token expira en 24 horas y solo puede usarse una vez.

**3. Iniciar sesión**

> `identifier` puede ser tu email o tu username.

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "tu_cuenta@example.com",
    "password": "miPassword123"
  }'
```

**Respuesta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "tu_cuenta@example.com",
  "onboardingCompleted": false
}
```

**4. Usar el token en endpoints protegidos**

```bash
curl -X GET http://localhost:8080/api/expenses \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

## Despliegue

El despliegue es **completamente automatizado** con GitHub Actions:

1. Push a `main` → corre tests con Maven.
2. Si pasan, GitHub Actions se conecta por SSH a la EC2.
3. La EC2 ejecuta `deploy.sh`: `git pull` → `docker build` → reemplaza el contenedor en caliente.

**Infraestructura en producción:**
- **API**: AWS EC2 (`t4g.small`) con Docker.
- **Base de datos**: AWS RDS MySQL.
- **Email**: Resend con dominio verificado.
- **Secretos**: variables de entorno cargadas desde `.env` en el host.

---

## Roadmap

- [ ] Más tests
- [ ] Refresh tokens
- [ ] Recuperación de contraseña
- [ ] Exportar gastos a CSV/PDF

---

## Autor

**Carlos Huarcaya**

- GitHub: [@carloshuarcayah](https://github.com/carloshuarcayah)     
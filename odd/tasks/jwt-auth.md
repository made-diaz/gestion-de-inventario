# Feature: autenticación con JWT

Objetivo: cerrar la API con login real y tokens JWT.
Rama: `feat/jwt-auth`. Runner: `.\gradlew.bat test`.

## Specs

- **S1.** `POST /api/auth/login` recibe `{ "usuario": "...", "password": "..." }` y devuelve `200` con `{ token, tipo, expiraEnSegundos, usuario, rol }` cuando las credenciales son válidas.
- **S2.** Credenciales inválidas devuelven `401` con `{ estado, mensaje }`. Usuario con `activoUsuario = false` devuelve `403`.
- **S3.** Todo `/api/**` excepto `/api/auth/login` exige `Authorization: Bearer <token>`. Sin token, token inválido o expirado devuelve `401` con cuerpo JSON.
- **S4.** El token es un JWT HS256 firmado con `app.jwt.secret`, con `subject` = nombre de usuario, claim `rol` y expiración `app.jwt.expiracion-minutos` (default 120). Un secreto de menos de 32 caracteres impide el arranque con mensaje claro.
- **S5.** `usuario` y `password` vacíos devuelven `400` (usa `spring-boot-starter-validation`, que estaba sin usar).
- **S6.** La contraseña se verifica con BCrypt contra el hash ya almacenado; nunca se compara en texto plano.
- **S7.** Si la tabla de usuarios está vacía, al arrancar se crea un usuario ADMIN a partir de `ADMIN_USER` / `ADMIN_PASSWORD`. Sin esas variables no se crea nada y queda un aviso en el log: no existe contraseña por defecto en el código.

## Tasks

- **T1.** `UsuarioDetailsService`, `JwtService`, `JwtAuthenticationFilter`. (S4, S6)
- **T2.** `AuthService`, `AuthController`, DTOs y validación. (S1, S5)
- **T3.** `SecurityConfig` stateless, reglas de autorización y respuestas JSON de 401/403. (S3)
- **T4.** Manejadores de error de autenticación en `GlobalExceptionHandler`. (S2)
- **T5.** Tests de login, credenciales inválidas, endpoint protegido con y sin token. (S1-S3)
- **T6.** `UsuarioInicialInitializer` para poder autenticarse en una base vacía. (S7)

## Log

- **L1.** Usuario: "ya continuamos con lo que falta+" — continuación del roadmap acordado, que arranca por autenticación.
- **L2.** Decisión: sesiones stateless, sin refresh token por ahora. El token vive 2 horas y se vuelve a pedir con login.
- **L3.** Detección durante la implementación: al exigir token en todos los endpoints, una base vacía quedaba inutilizable (no había forma de crear el primer usuario). Se agregó el usuario inicial por variables de entorno.

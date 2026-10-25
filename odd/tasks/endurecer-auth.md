# Feature: endurecer autenticación y cuentas

Objetivo: que el login no se pueda martillar, que las contraseñas tengan un mínimo real y que el sistema no quede sin administrador.
Rama: `feat/endurecer-auth`. Runner: `.\\gradlew.bat test`.

## Specs

- **S1.** Tras `app.login.max-intentos` (5) fallos para el mismo usuario desde la misma IP, el login responde `429` con `{ estado, mensaje }` y header `Retry-After` en segundos. El bloqueo dura `app.login.bloqueo-minutos` (15).
- **S2.** Un login exitoso borra el historial de fallos de ese usuario + IP. El bloqueo de un usuario no afecta a otros usuarios ni a otras IPs.
- **S3.** Al crear un usuario la contraseña debe tener al menos 8 caracteres, no ser igual al nombre de usuario y combinar letras y números. Si no cumple, `400`.
- **S4.** Un usuario deshabilitado, uno inexistente y una contraseña incorrecta devuelven exactamente la misma respuesta `401`: no se puede averiguar qué usuarios existen.
- **S5.** No se puede eliminar el último administrador activo: `409` con mensaje explicativo. Un administrador inactivo no cuenta para esa guarda.

## Tasks

- **T1.** `IntentoLoginService` con reloj inyectable y bloqueo por usuario + IP. (S1, S2)
- **T2.** `UsuarioService.validarPassword` y guarda del último administrador. (S3, S5)
- **T3.** Unificación del mensaje de autenticación y mapeo `429`. (S4)
- **T4.** Tests: unitarios de bloqueo con reloj movido, unitarios de la guarda, integración de 429/400/401. (S1-S5)

## Log

- **L1.** Usuario: "sigue con la seguridad" — tras el reparto de roles, falta el endurecimiento del login y de las cuentas.
- **L2.** Decisión: el contador de fallos es por usuario + IP, no solo por usuario. Si fuera solo por usuario, cualquiera podría dejar afuera al admin intentando loguearse con su nombre desde otra máquina.
- **L3.** Decisión: el estado en memoria alcanza para una instancia. Con más de una, el contador se mueve a Redis o a la base.
- **L4.** Decisión: un usuario deshabilitado ya no devuelve `403` al loguear; devuelve el mismo `401` que una contraseña mala. La comodidad de saber "estás deshabilitado" le regala al atacante la lista de usuarios válidos.

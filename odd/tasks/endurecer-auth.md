# Feature: endurecer autenticación y cuentas

Objetivo: que el login no se pueda martillar, que las contraseñas tengan un mínimo real y que el sistema no quede sin administrador.
Rama: `feat/endurecer-auth`. Runner: `.\\gradlew.bat test`.

## Specs

- **S1.** Tras `app.login.max-intentos` (5) intentos para el mismo usuario desde la misma IP, el login responde `429` con `{ estado, mensaje }` y header `Retry-After` en segundos. El bloqueo dura `app.login.bloqueo-minutos` (15).
- **S2.** El intento se reserva antes de verificar la contraseña, de forma atómica: una ráfaga de 10 requests simultáneos no consigue más de 5 verificaciones reales. Un login exitoso borra el historial de ese usuario + IP. El bloqueo de un usuario no afecta a otros usuarios ni a otras IPs.
- **S3.** Al crear un usuario la contraseña debe tener al menos 8 caracteres, no ser igual al nombre de usuario y combinar letras y números. Si no cumple, `400`.
- **S4.** Un usuario deshabilitado, uno inexistente y una contraseña incorrecta devuelven exactamente la misma respuesta `401`: no se puede averiguar qué usuarios existen.
- **S5.** No se puede eliminar el último administrador activo: `409` con mensaje explicativo. Un administrador inactivo no cuenta para esa guarda. Borrar un id inexistente sigue siendo `204`.
- **S6.** Si `ADMIN_PASSWORD` no cumple la política mínima y la base está vacía, la aplicación no arranca y lo dice en el log, en vez de quedar arriba sin ningún usuario.

## Tasks

- **T1.** `IntentoLoginService` con reloj inyectable, reserva atómica y bloqueo por usuario + IP. (S1, S2)
- **T2.** `UsuarioService.validarPassword` y guarda del último administrador con el commit dentro del lock. (S3, S5)
- **T3.** Unificación del mensaje de autenticación, mapeo `429` y `409`. (S4, S5)
- **T4.** `UsuarioInicialInitializer` falla ruidosamente con una contraseña débil. (S6)
- **T5.** Tests: unitarios de bloqueo con reloj movido y ráfaga concurrente, unitarios de la guarda, integración de `429`/`400`/`401`/`409`/`204`. (S1-S6)

## Log

- **L1.** Usuario: "sigue con la seguridad" — tras el reparto de roles, falta el endurecimiento del login y de las cuentas.
- **L2.** Decisión: el contador de fallos es por usuario + IP, no solo por usuario. Si fuera solo por usuario, cualquiera podría dejar afuera al admin intentando loguearse con su nombre desde otra máquina.
- **L3.** Decisión: el estado en memoria alcanza para una instancia. Con más de una, el contador se mueve a Redis o a la base, y la guarda del último admin a un lock de base.
- **L4.** Decisión: un usuario deshabilitado ya no devuelve `403` al loguear; devuelve el mismo `401` que una contraseña mala. La comodidad de saber "estás deshabilitado" le regala al atacante la lista de usuarios válidos.
- **L5.** Verificación independiente sobre `7975606` encontró 5 blockers: (1) borrar el último admin devolvía `400` en vez de `409`; (2) 10 logins en paralelo daban 10 oportunidades reales porque el fallo se contaba después de autenticar; (3) dos borrados simultáneos de admin dejaban cero administradores porque el chequeo y el commit estaban fuera del lock; (4) `ADMIN_PASSWORD` débil arrancaba sin crear ningún usuario, en silencio; (5) `DELETE` de un id inexistente pasó de `204` a `404` sin que nadie lo pidiera. Se corrigieron los cinco: excepción propia con `409`, reserva del intento antes de autenticar, `TransactionTemplate` dentro del lock, arranque fallido con mensaje claro y no-op para el id inexistente.
- **L6.** Advisories que quedan abiertos y se aceptan con los ojos abiertos: el tiempo de respuesta todavía distingue una cuenta deshabilitada (~4 ms) de una inexistente (~65 ms), `remoteAddr` queda detrás de un proxy inverso si algún día se publica así, el contador no tiene ventana corregida (5 fallos viejos siguen contando) y el mapa se acota descartando registros no bloqueados al pasar 10.000 claves.

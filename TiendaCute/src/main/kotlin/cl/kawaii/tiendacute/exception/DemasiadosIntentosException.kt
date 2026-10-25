package cl.kawaii.tiendacute.exception

/**
 * Se lanza cuando se superaron los intentos de login permitidos para un
 * usuario desde una misma IP. Se responde 429 con Retry-After.
 */
class DemasiadosIntentosException(
    val reintentarEnSegundos: Long,
    mensaje: String
) : RuntimeException(mensaje)

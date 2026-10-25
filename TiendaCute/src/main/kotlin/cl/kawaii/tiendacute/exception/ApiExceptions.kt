package cl.kawaii.tiendacute.exception

/**
 * Se lanza cuando el recurso solicitado no existe.
 * El manejador global la traduce a HTTP 404.
 */
class RecursoNoEncontradoException(mensaje: String) : RuntimeException(mensaje)

/**
 * Se lanza cuando la solicitud es válida en forma pero viola una regla de negocio
 * (stock insuficiente, producto inactivo, cantidades inválidas).
 * El manejador global la traduce a HTTP 400.
 */
class ReglaNegocioException(mensaje: String) : RuntimeException(mensaje)

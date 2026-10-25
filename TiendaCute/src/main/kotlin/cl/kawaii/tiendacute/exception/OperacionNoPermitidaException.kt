package cl.kawaii.tiendacute.exception

/**
 * La operación es válida en forma pero dejaría el sistema en un estado
 * inaceptable (por ejemplo, sin ningún administrador). Se responde 409.
 */
class OperacionNoPermitidaException(mensaje: String) : RuntimeException(mensaje)

package cl.kawaii.tiendacute.exception

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

data class ErrorResponse(val estado: Int, val mensaje: String)

/**
 * Traduce las excepciones de dominio y de autenticación a respuestas HTTP con
 * un cuerpo uniforme, en lugar de dejar que todo termine como un 500 genérico.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException::class)
    fun manejarNoEncontrado(ex: RecursoNoEncontradoException): ResponseEntity<ErrorResponse> =
        construir(HttpStatus.NOT_FOUND, ex.message)

    @ExceptionHandler(ReglaNegocioException::class)
    fun manejarReglaDeNegocio(ex: ReglaNegocioException): ResponseEntity<ErrorResponse> =
        construir(HttpStatus.BAD_REQUEST, ex.message)

    @ExceptionHandler(BadCredentialsException::class)
    fun manejarCredencialesInvalidas(ex: BadCredentialsException): ResponseEntity<ErrorResponse> =
        construir(HttpStatus.UNAUTHORIZED, MENSAJE_CREDENCIALES)

    @ExceptionHandler(AuthenticationException::class)
    fun manejarAutenticacion(ex: AuthenticationException): ResponseEntity<ErrorResponse> =
        construir(HttpStatus.UNAUTHORIZED, MENSAJE_CREDENCIALES)

    @ExceptionHandler(DemasiadosIntentosException::class)
    fun manejarDemasiadosIntentos(ex: DemasiadosIntentosException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .header(HttpHeaders.RETRY_AFTER, ex.reintentarEnSegundos.toString())
            .body(ErrorResponse(HttpStatus.TOO_MANY_REQUESTS.value(), ex.message ?: "Demasiados intentos"))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun manejarValidacion(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val detalle = ex.bindingResult.fieldErrors
            .firstOrNull()
            ?.let { "${it.field}: ${it.defaultMessage}" }
            ?: "Solicitud inválida"
        return construir(HttpStatus.BAD_REQUEST, detalle)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun manejarCuerpoIlegible(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
        construir(HttpStatus.BAD_REQUEST, "Cuerpo de la solicitud inválido o con campos obligatorios faltantes")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun manejarParametroInvalido(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> =
        construir(HttpStatus.BAD_REQUEST, "El parámetro '${ex.name}' tiene un formato inválido")

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun manejarConflictoDeDatos(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> =
        construir(
            HttpStatus.CONFLICT,
            "No se puede completar: el registro tiene datos asociados o repite un valor único"
        )

    private fun construir(estado: HttpStatus, mensaje: String?): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(estado).body(ErrorResponse(estado.value(), mensaje ?: estado.reasonPhrase))

    private companion object {
        // Un mismo mensaje para usuario inexistente, contraseña incorrecta y
        // cuenta deshabilitada: no se le confirma al atacante qué falló.
        const val MENSAJE_CREDENCIALES = "Usuario o contraseña incorrectos"
    }
}

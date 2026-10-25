package cl.kawaii.tiendacute.exception

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException

data class ErrorResponse(val estado: Int, val mensaje: String)

/**
 * Traduce las excepciones de dominio a respuestas HTTP con un cuerpo uniforme,
 * en lugar de dejar que todo termine como un 500 genérico.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException::class)
    fun manejarNoEncontrado(ex: RecursoNoEncontradoException): ResponseEntity<ErrorResponse> =
        construir(HttpStatus.NOT_FOUND, ex.message)

    @ExceptionHandler(ReglaNegocioException::class)
    fun manejarReglaDeNegocio(ex: ReglaNegocioException): ResponseEntity<ErrorResponse> =
        construir(HttpStatus.BAD_REQUEST, ex.message)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun manejarCuerpoIlegible(ex: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> =
        construir(HttpStatus.BAD_REQUEST, "Cuerpo de la solicitud inválido o con campos obligatorios faltantes")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun manejarParametroInvalido(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> =
        construir(HttpStatus.BAD_REQUEST, "El parámetro '${ex.name}' tiene un formato inválido")

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun manejarConflictoDeDatos(ex: DataIntegrityViolationException): ResponseEntity<ErrorResponse> =
        construir(HttpStatus.CONFLICT, "La operación viola una restricción de la base de datos")

    private fun construir(estado: HttpStatus, mensaje: String?): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(estado).body(ErrorResponse(estado.value(), mensaje ?: estado.reasonPhrase))
}

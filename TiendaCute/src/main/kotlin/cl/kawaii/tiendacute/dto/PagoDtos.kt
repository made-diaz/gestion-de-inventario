package cl.kawaii.tiendacute.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class PagoRequest(
    val ventaId: Long,
    val usuarioId: Long,
    @field:NotBlank(message = "El método de pago es obligatorio")
    @field:Size(max = 50, message = "El método de pago no puede superar los 50 caracteres")
    val metodoPago: String,

    @field:Positive(message = "El monto del pago debe ser mayor a cero")
    val montoPago: BigDecimal,

    val referencia: String? = null
)

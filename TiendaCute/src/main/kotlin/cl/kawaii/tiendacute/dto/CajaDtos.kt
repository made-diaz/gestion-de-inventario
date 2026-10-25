package cl.kawaii.tiendacute.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class AbrirCajaRequest(
    val usuarioId: Long,
    @field:DecimalMin(value = "0", message = "El monto inicial no puede ser negativo")
    val montoInicial: BigDecimal
)

data class CerrarCajaRequest(
    val usuarioId: Long,
    @field:DecimalMin(value = "0", message = "El monto final no puede ser negativo")
    val montoFinal: BigDecimal
)

data class EmitirDocumentoRequest(
    val ventaId: Long,
    @field:NotBlank(message = "El tipo de documento es obligatorio")
    val tipo: String
)

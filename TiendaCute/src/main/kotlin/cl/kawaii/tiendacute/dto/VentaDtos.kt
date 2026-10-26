package cl.kawaii.tiendacute.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDateTime

/** DTO liviano para el listado de ventas (GET /api/ventas). */
data class VentaListDto(
    val idVenta: Long,
    val fechaVenta: LocalDateTime?,
    val tipoDocumento: String,
    val total: BigDecimal,
    val cantidadLineas: Long,
    val clienteNombre: String?,
    val usuarioNombre: String
)

/** DTO detallado de una venta (GET /api/ventas/{id}). */
data class VentaDetalleDto(
    val idVenta: Long,
    val fechaVenta: LocalDateTime?,
    val tipoDocumento: String,
    val estadoVenta: String,
    val totalNeto: BigDecimal,
    val iva: BigDecimal,
    val total: BigDecimal,
    val montoPagado: BigDecimal,
    val saldoPendiente: BigDecimal,
    val cajero: CajeroDto,
    val cliente: ClienteDto?,
    val documento: DocumentoDto?,
    val pagos: List<PagoDto>,
    val lineas: List<LineaVentaDto>
)

data class CajeroDto(val id: Long, val nombre: String)

data class ClienteDto(val id: Long, val nombre: String?, val rut: String?)

data class DocumentoDto(val folio: Int?, val estadoSii: String?)

data class PagoDto(
    val metodoPago: String,
    val montoPago: BigDecimal,
    val referencia: String?,
    val creadoEn: LocalDateTime?
)

data class LineaVentaDto(
    val idProducto: Long,
    val nombreProducto: String,
    val cantidad: Int,
    val precioUnitario: BigDecimal,
    val subtotal: BigDecimal
)

/** Pago opcional enviado al registrar una venta. */
data class PagoVentaRequest(
    @field:NotBlank(message = "El método de pago es obligatorio")
    val metodoPago: String,
    @field:Positive(message = "El monto del pago debe ser mayor a cero")
    val montoPago: BigDecimal,
    val referencia: String? = null
)

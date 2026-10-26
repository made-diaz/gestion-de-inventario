package cl.kawaii.tiendacute.dto

/**
 * Payload de entrada para registrar una venta.
 *
 * El cliente solo indica qué se vende (producto y cantidad) y quién lo vende.
 * Los precios, subtotales, neto, IVA y total los calcula el servidor a partir
 * del catálogo, para que no puedan ser manipulados desde afuera.
 *
 * Los pagos son opcionales y se envían como una lista de PagoVentaRequest.
 */
data class VentaRequest(
    val venta: DatosVenta,
    val detalles: List<DetalleVentaRequest>,
    val usuarioId: Long,
    val clienteRut: String? = null,
    val pagos: List<cl.kawaii.tiendacute.dto.PagoVentaRequest> = emptyList()
)

data class DatosVenta(
    val tipoDocumento: String,
    val estadoVenta: String = "COMPLETADA"
)

data class DetalleVentaRequest(
    val productoId: Long,
    val cantidad: Int
)

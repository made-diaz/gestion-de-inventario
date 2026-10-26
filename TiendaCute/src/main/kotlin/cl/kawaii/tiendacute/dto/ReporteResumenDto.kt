package cl.kawaii.tiendacute.dto

import java.math.BigDecimal
import java.time.LocalDate

data class ReporteResumenDto(
    val vendidoHoy: BigDecimal,
    val cantidadVentasHoy: Int,
    val productosStockBajo: List<ProductoStockBajoDto>,
    val ventasPorDia: List<VentaPorDiaDto>
)

data class ProductoStockBajoDto(
    val idProducto: Long,
    val nombreProducto: String,
    val codigoProducto: String,
    val stockActual: Int
)

data class VentaPorDiaDto(
    val fecha: LocalDate,
    val total: BigDecimal,
    val cantidad: Int
)

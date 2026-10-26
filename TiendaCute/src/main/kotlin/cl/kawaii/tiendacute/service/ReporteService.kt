package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.ProductoStockBajoDto
import cl.kawaii.tiendacute.dto.ReporteResumenDto
import cl.kawaii.tiendacute.dto.VentaPorDiaDto
import cl.kawaii.tiendacute.repository.ProductoRepository
import cl.kawaii.tiendacute.repository.VentaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class ReporteService(
    private val ventaRepository: VentaRepository,
    private val productoRepository: ProductoRepository
) {

    private companion object {
        const val ZONA_SANTIAGO = "America/Santiago"
        const val MAX_PRODUCTOS_STOCK_BAJO = 10
    }

    /**
     * Resumen del día (zona America/Santiago) y productos con stock bajo.
     * Todas las agregaciones se hacen en base de datos mediante consultas nativas.
     */
    @Transactional(readOnly = true)
    fun obtenerResumen(umbralStock: Int): ReporteResumenDto {
        val zona = ZoneId.of(ZONA_SANTIAGO)
        val ahora = ZonedDateTime.now(zona)
        val inicioDelDia = ahora.toLocalDate().atStartOfDay(zona).toLocalDateTime()
        val finDelDia = ahora.toLocalDate().plusDays(1).atStartOfDay(zona).toLocalDateTime()

        // vendidoHoy y cantidadVentasHoy (agregado en BD)
        val filaDia = ventaRepository.obtenerResumenDia(inicioDelDia, finDelDia).firstOrNull()
        val vendidoHoy = if (filaDia == null || filaDia[0] == null) BigDecimal.ZERO else filaDia[0] as BigDecimal
        val cantidadVentasHoy = if (filaDia == null) 0 else (filaDia[1] as Number).toInt()

        // productosStockBajo (agregado en BD)
        val filasStockBajo = productoRepository.obtenerProductosStockBajo(umbralStock)
        val productosStockBajo = filasStockBajo.map { fila ->
            ProductoStockBajoDto(
                idProducto = fila[0] as Long,
                nombreProducto = fila[1] as String,
                codigoProducto = fila[2] as String,
                stockActual = (fila[3] as Number).toInt()
            )
        }

        // ventasPorDia últimos 7 días (agregado en BD)
        val inicioSemana = ahora.toLocalDate().minusDays(6).atStartOfDay(zona).toLocalDateTime()
        val filasVentasPorDia = ventaRepository.obtenerVentasPorDia(inicioSemana, finDelDia)
        val ventasPorDia = filasVentasPorDia.map { fila ->
            VentaPorDiaDto(
                fecha = (fila[0] as java.sql.Date).toLocalDate(),
                total = fila[1] as BigDecimal,
                cantidad = (fila[2] as Number).toInt()
            )
        }

        return ReporteResumenDto(
            vendidoHoy = vendidoHoy,
            cantidadVentasHoy = cantidadVentasHoy,
            productosStockBajo = productosStockBajo,
            ventasPorDia = ventasPorDia
        )
    }
}

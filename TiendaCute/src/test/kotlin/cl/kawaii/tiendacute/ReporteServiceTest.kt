package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.dto.DatosVenta
import cl.kawaii.tiendacute.dto.DetalleVentaRequest
import cl.kawaii.tiendacute.dto.VentaRequest
import cl.kawaii.tiendacute.model.Categoria
import cl.kawaii.tiendacute.model.Producto
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.CategoriaRepository
import cl.kawaii.tiendacute.repository.ProductoRepository
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.repository.VentaRepository
import cl.kawaii.tiendacute.service.ReporteService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class ReporteServiceTest {

    @Autowired
    private lateinit var reporteService: ReporteService

    @Autowired
    private lateinit var ventaService: cl.kawaii.tiendacute.service.VentaService

    @Autowired
    private lateinit var ventaRepository: VentaRepository

    @Autowired
    private lateinit var productoRepository: ProductoRepository

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var categoriaRepository: CategoriaRepository

    @Test
    fun `resumen del día muestra ventas del día y productos con stock bajo`() {
        val usuario = crearUsuario("reporte-vendedor")
        val categoria = categoriaRepository.save(Categoria(nombreCategoria = "Cat reporte"))

        // Productos con stock bajo (<= 5)
        val prodStock1 = productoRepository.save(
            Producto(
                nombreProducto = "Producto stock 1",
                codigoProducto = "REP-001",
                precioCompraProducto = BigDecimal("100.00"),
                precioVentaProducto = BigDecimal("200.00"),
                stockActualProducto = 1,
                categoria = categoria
            )
        )
        val prodStock2 = productoRepository.save(
            Producto(
                nombreProducto = "Producto stock 2",
                codigoProducto = "REP-002",
                precioCompraProducto = BigDecimal("100.00"),
                precioVentaProducto = BigDecimal("200.00"),
                stockActualProducto = 2,
                categoria = categoria
            )
        )
        val prodStock5 = productoRepository.save(
            Producto(
                nombreProducto = "Producto stock 5",
                codigoProducto = "REP-005",
                precioCompraProducto = BigDecimal("100.00"),
                precioVentaProducto = BigDecimal("200.00"),
                stockActualProducto = 5,
                categoria = categoria
            )
        )
        // Producto con stock normal (no debe aparecer en stock bajo con umbral 5)
        val prodNormal = productoRepository.save(
            Producto(
                nombreProducto = "Producto normal",
                codigoProducto = "REP-100",
                precioCompraProducto = BigDecimal("100.00"),
                precioVentaProducto = BigDecimal("200.00"),
                stockActualProducto = 100,
                categoria = categoria
            )
        )

        // Venta de hoy con total 5000
        val venta = ventaService.registrarVenta(
            VentaRequest(
                venta = DatosVenta(tipoDocumento = "BOLETA"),
                detalles = listOf(
                    DetalleVentaRequest(productoId = prodStock1.idProducto, cantidad = 1),
                    DetalleVentaRequest(productoId = prodNormal.idProducto, cantidad = 1)
                ),
                usuarioId = usuario.idUsuario
            )
        )

        val resumen = reporteService.obtenerResumen(umbralStock = 5)

        // Vendido hoy incluye al menos nuestra venta registrada (puede haber más de otros tests)
        assertTrue(resumen.vendidoHoy >= venta.total, "vendidoHoy ${resumen.vendidoHoy} debe ser >= ${venta.total}")
        assertTrue(resumen.cantidadVentasHoy >= 1, "cantidadVentasHoy ${resumen.cantidadVentasHoy} debe ser >= 1")

        // Nuestros productos de stock bajo deben estar en el listado (stock <= 5)
        val nuestrosSkus = listOf("REP-001", "REP-002", "REP-005")
        val skusEnResumen = resumen.productosStockBajo.map { it.codigoProducto }
        for (sku in nuestrosSkus) {
            assertTrue(skusEnResumen.contains(sku), "Producto $sku debe estar en stock bajo")
        }
        // Todos los productos en stock bajo deben tener stock <= umbral
        assertTrue(resumen.productosStockBajo.all { it.stockActual <= 5 })

        // Ventas por día: al menos el día de hoy con la venta registrada
        assertTrue(resumen.ventasPorDia.isNotEmpty())
        val hoy = resumen.ventasPorDia.find { it.total == venta.total }
        assertTrue(hoy != null, "Debería haber una entrada para hoy con total ${venta.total}")
        assertTrue(hoy!!.cantidad >= 1)
    }

    @Test
    fun `resumen sin ventas del día devuelve ceros`() {
        val resumen = reporteService.obtenerResumen(umbralStock = 5)

        assertEquals(BigDecimal.ZERO, resumen.vendidoHoy)
        assertEquals(0, resumen.cantidadVentasHoy)
        assertTrue(resumen.ventasPorDia.isEmpty())
    }

    private fun crearUsuario(nombre: String): Usuario =
        usuarioRepository.save(Usuario(usuario = nombre, password = "secreto", rolUsuario = "VENDEDOR"))
}

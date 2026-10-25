package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.dto.DatosVenta
import cl.kawaii.tiendacute.dto.DetalleVentaRequest
import cl.kawaii.tiendacute.dto.VentaRequest
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.Categoria
import cl.kawaii.tiendacute.model.Producto
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.CategoriaRepository
import cl.kawaii.tiendacute.repository.MovimientoStockRepository
import cl.kawaii.tiendacute.repository.ProductoRepository
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.repository.VentaRepository
import cl.kawaii.tiendacute.service.VentaService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest
class VentaServiceTest {

    @Autowired
    private lateinit var ventaService: VentaService

    @Autowired
    private lateinit var ventaRepository: VentaRepository

    @Autowired
    private lateinit var productoRepository: ProductoRepository

    @Autowired
    private lateinit var categoriaRepository: CategoriaRepository

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var movimientoStockRepository: MovimientoStockRepository

    @Test
    fun `registrar venta descuenta stock, calcula totales y deja movimiento de salida`() {
        val usuario = crearUsuario("vendedor-totales")
        val producto = crearProducto("SKU-TOTALES", stock = 10, precioVenta = "1000.00")

        val venta = ventaService.registrarVenta(
            VentaRequest(
                venta = DatosVenta(tipoDocumento = "BOLETA"),
                detalles = listOf(DetalleVentaRequest(productoId = producto.idProducto, cantidad = 3)),
                usuarioId = usuario.idUsuario
            )
        )

        assertEquals(BigDecimal("3000.00"), venta.totalNeto)
        assertEquals(BigDecimal("570.00"), venta.iva)
        assertEquals(BigDecimal("3570.00"), venta.total)

        val productoDespues = productoRepository.findById(producto.idProducto).orElseThrow()
        assertEquals(7, productoDespues.stockActualProducto)

        val movimientos = movimientoStockRepository.findByProducto_IdProducto(producto.idProducto)
        assertEquals(1, movimientos.size)
        assertEquals("SALIDA", movimientos.single().tipoMovimiento)
        assertEquals("VENTA", movimientos.single().motivoMovimiento)
        assertEquals(3, movimientos.single().cantidadMovimiento)
    }

    @Test
    fun `registrar venta suma cantidades repetidas del mismo producto`() {
        val usuario = crearUsuario("vendedor-repetidos")
        val producto = crearProducto("SKU-REPETIDOS", stock = 10, precioVenta = "500.00")

        val venta = ventaService.registrarVenta(
            VentaRequest(
                venta = DatosVenta(tipoDocumento = "BOLETA"),
                detalles = listOf(
                    DetalleVentaRequest(productoId = producto.idProducto, cantidad = 2),
                    DetalleVentaRequest(productoId = producto.idProducto, cantidad = 3)
                ),
                usuarioId = usuario.idUsuario
            )
        )

        assertEquals(BigDecimal("2500.00"), venta.totalNeto)
        assertEquals(5, productoRepository.findById(producto.idProducto).orElseThrow().stockActualProducto)
    }

    @Test
    fun `registrar venta rechaza stock insuficiente sin descontar nada`() {
        val usuario = crearUsuario("vendedor-sin-stock")
        val producto = crearProducto("SKU-SIN-STOCK", stock = 2, precioVenta = "1000.00")
        val ventasAntes = ventaRepository.count()

        val error = assertFailsWith<ReglaNegocioException> {
            ventaService.registrarVenta(
                VentaRequest(
                    venta = DatosVenta(tipoDocumento = "BOLETA"),
                    detalles = listOf(DetalleVentaRequest(productoId = producto.idProducto, cantidad = 5)),
                    usuarioId = usuario.idUsuario
                )
            )
        }

        assertTrue(error.message!!.contains("Stock insuficiente"))
        assertEquals(2, productoRepository.findById(producto.idProducto).orElseThrow().stockActualProducto)
        assertEquals(ventasAntes, ventaRepository.count())
        assertTrue(movimientoStockRepository.findByProducto_IdProducto(producto.idProducto).isEmpty())
    }

    @Test
    fun `registrar venta rechaza un detalle vacio`() {
        val usuario = crearUsuario("vendedor-sin-detalles")

        assertFailsWith<ReglaNegocioException> {
            ventaService.registrarVenta(
                VentaRequest(
                    venta = DatosVenta(tipoDocumento = "BOLETA"),
                    detalles = emptyList(),
                    usuarioId = usuario.idUsuario
                )
            )
        }
    }

    @Test
    fun `registrar venta rechaza un usuario inexistente`() {
        val producto = crearProducto("SKU-SIN-USUARIO", stock = 5, precioVenta = "100.00")

        assertFailsWith<RecursoNoEncontradoException> {
            ventaService.registrarVenta(
                VentaRequest(
                    venta = DatosVenta(tipoDocumento = "BOLETA"),
                    detalles = listOf(DetalleVentaRequest(productoId = producto.idProducto, cantidad = 1)),
                    usuarioId = 999_999
                )
            )
        }
    }

    private fun crearUsuario(nombre: String): Usuario =
        usuarioRepository.save(
            Usuario(usuario = nombre, password = "secreto", rolUsuario = "VENDEDOR")
        )

    private fun crearProducto(codigo: String, stock: Int, precioVenta: String): Producto {
        val categoria = categoriaRepository.save(Categoria(nombreCategoria = "Categoría $codigo"))
        return productoRepository.save(
            Producto(
                nombreProducto = "Producto $codigo",
                codigoProducto = codigo,
                precioCompraProducto = BigDecimal("500.00"),
                precioVentaProducto = BigDecimal(precioVenta),
                stockActualProducto = stock,
                categoria = categoria
            )
        )
    }
}

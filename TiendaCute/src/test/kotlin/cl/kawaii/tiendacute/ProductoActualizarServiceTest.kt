package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.dto.ProductoActualizarRequest
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.MovimientoStock
import cl.kawaii.tiendacute.model.Producto
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.CategoriaRepository
import cl.kawaii.tiendacute.repository.MovimientoStockRepository
import cl.kawaii.tiendacute.repository.ProductoRepository
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.service.ProductoService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
class ProductoActualizarServiceTest {

    @Autowired
    private lateinit var productoService: ProductoService

    @Autowired
    private lateinit var productoRepository: ProductoRepository

    @Autowired
    private lateinit var movimientoStockRepository: MovimientoStockRepository

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var categoriaRepository: CategoriaRepository

    @Test
    fun `actualizar producto cambia nombre y precio`() {
        val categoria = categoriaRepository.save(cl.kawaii.tiendacute.model.Categoria(nombreCategoria = "Cat prod upd"))
        val producto = productoRepository.save(
            Producto(
                nombreProducto = "Viejo",
                codigoProducto = "UPD-001",
                precioCompraProducto = BigDecimal("100.00"),
                precioVentaProducto = BigDecimal("200.00"),
                stockActualProducto = 5,
                categoria = categoria
            )
        )

        val actualizado = productoService.actualizarProducto(
            producto.idProducto,
            ProductoActualizarRequest(
                nombreProducto = "Nuevo nombre",
                precioVentaProducto = BigDecimal("250.00")
            ),
            usuarioId = crearUsuario("upd-user").idUsuario
        )

        assertEquals("Nuevo nombre", actualizado.nombreProducto)
        assertEquals(BigDecimal("250.00"), actualizado.precioVentaProducto)
        assertEquals(BigDecimal("100.00"), actualizado.precioCompraProducto) // no cambió
        assertEquals(5, actualizado.stockActualProducto) // no cambió
    }

    @Test
    fun `editar stock deja movimiento de ajuste`() {
        val categoria = categoriaRepository.save(cl.kawaii.tiendacute.model.Categoria(nombreCategoria = "Cat stock upd"))
        val producto = productoRepository.save(
            Producto(
                nombreProducto = "Stock",
                codigoProducto = "UPD-STK",
                precioCompraProducto = BigDecimal("100.00"),
                precioVentaProducto = BigDecimal("200.00"),
                stockActualProducto = 10,
                categoria = categoria
            )
        )
        val usuario = crearUsuario("upd-stock")

        productoService.actualizarProducto(
            producto.idProducto,
            ProductoActualizarRequest(stockActualProducto = 4),
            usuarioId = usuario.idUsuario
        )

        val despues = productoRepository.findById(producto.idProducto).orElseThrow()
        assertEquals(4, despues.stockActualProducto)

        val movimientos = movimientoStockRepository.findByProducto_IdProducto(producto.idProducto)
        assertEquals(1, movimientos.size)
        assertEquals("AJUSTE_STOCK", movimientos.single().tipoMovimiento)
        assertEquals(-6, movimientos.single().cantidadMovimiento)
        assertEquals("AJUSTE_MANUAL", movimientos.single().motivoMovimiento)
        assertEquals(usuario.idUsuario, movimientos.single().usuario.idUsuario)
    }

    private fun crearUsuario(nombre: String): Usuario =
        usuarioRepository.save(Usuario(usuario = nombre, password = "secreto", rolUsuario = "VENDEDOR"))
}

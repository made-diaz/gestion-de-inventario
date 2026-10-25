package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.dto.PagoRequest
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.Pago
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.model.Venta
import cl.kawaii.tiendacute.repository.CajaRepository
import cl.kawaii.tiendacute.repository.MovimientoCajaRepository
import cl.kawaii.tiendacute.repository.PagoRepository
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.repository.VentaRepository
import cl.kawaii.tiendacute.service.PagoService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class PagoServiceTest {

    @Autowired
    private lateinit var pagoService: PagoService

    @Autowired
    private lateinit var pagoRepository: PagoRepository

    @Autowired
    private lateinit var ventaRepository: VentaRepository

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var cajaRepository: CajaRepository

    @Autowired
    private lateinit var movimientoCajaRepository: MovimientoCajaRepository

    @Test
    fun `registra un pago y reduce el saldo pendiente de la venta`() {
        val usuario = crearUsuario("pago-vendedor")
        val venta = crearVenta("pago-venta-1", usuario, BigDecimal("1000.00"))

        val pago = pagoService.guardarPago(
            PagoRequest(venta.idVenta, usuario.idUsuario, "EFECTIVO", BigDecimal("400.00"), "abono")
        )

        assertEquals(BigDecimal("400.00"), pago.montoPago)
        assertEquals(BigDecimal("600.00"), ventaRepository.findById(venta.idVenta).orElseThrow().saldoPendiente())
    }

    @Test
    fun `rechaza un pago que excede el saldo pendiente`() {
        val usuario = crearUsuario("pago-vendedor-2")
        val venta = crearVenta("pago-venta-2", usuario, BigDecimal("1000.00"))

        assertFailsWith<ReglaNegocioException> {
            pagoService.guardarPago(
                PagoRequest(venta.idVenta, usuario.idUsuario, "EFECTIVO", BigDecimal("1500.00"))
            )
        }

        // La venta no se tocó y no se creó ningún pago.
        assertEquals(BigDecimal("1000.00"), ventaRepository.findById(venta.idVenta).orElseThrow().saldoPendiente())
        assertTrue(pagoRepository.findAll().none { it.venta.idVenta == venta.idVenta })
    }

    @Test
    fun `rechaza un pago contra una venta inexistente`() {
        val usuario = crearUsuario("pago-vendedor-3")

        assertFailsWith<RecursoNoEncontradoException> {
            pagoService.guardarPago(
                PagoRequest(999_999, usuario.idUsuario, "EFECTIVO", BigDecimal("100.00"))
            )
        }
    }

    @Test
    fun `admite pagos parciales hasta completar la venta`() {
        val usuario = crearUsuario("pago-vendedor-4")
        val venta = crearVenta("pago-venta-3", usuario, BigDecimal("1000.00"))

        pagoService.guardarPago(PagoRequest(venta.idVenta, usuario.idUsuario, "EFECTIVO", BigDecimal("300.00")))
        pagoService.guardarPago(PagoRequest(venta.idVenta, usuario.idUsuario, "TARJETA", BigDecimal("700.00")))

        assertTrue(ventaRepository.findById(venta.idVenta).orElseThrow().saldoPendiente().compareTo(BigDecimal.ZERO) == 0)
        assertEquals(2, pagoRepository.findAll().count { it.venta.idVenta == venta.idVenta })
    }

    @Test
    fun `registra movimiento de caja cuando hay una caja abierta`() {
        val usuario = crearUsuario("pago-vendedor-5")
        val venta = crearVenta("pago-venta-4", usuario, BigDecimal("500.00"))
        cajaRepository.save(
            cl.kawaii.tiendacute.model.Caja(
                montoInicial = BigDecimal("100.00"),
                estado = "ABIERTA",
                usuario = usuario
            )
        )

        pagoService.guardarPago(PagoRequest(venta.idVenta, usuario.idUsuario, "EFECTIVO", BigDecimal("500.00")))

        val movimientos = movimientoCajaRepository.findAll()
        assertEquals(1, movimientos.size)
        assertEquals(BigDecimal("500.00"), movimientos.single().monto)
        assertEquals("INGRESO", movimientos.single().tipoMovimiento)
        assertEquals("PAGO", movimientos.single().referenciaTipo)
    }

    private fun crearUsuario(nombre: String): Usuario =
        usuarioRepository.save(Usuario(usuario = nombre, password = "secreto", rolUsuario = "VENDEDOR"))

    private fun crearVenta(codigo: String, usuario: Usuario, total: BigDecimal): Venta =
        ventaRepository.save(
            Venta(
                tipoDocumento = "BOLETA",
                estadoVenta = "COMPLETADA",
                total = total,
                montoPagado = BigDecimal.ZERO,
                usuario = usuario
            )
        )
}

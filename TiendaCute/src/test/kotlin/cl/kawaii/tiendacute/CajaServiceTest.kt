package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.exception.OperacionNoPermitidaException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.Caja
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.CajaRepository
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.service.CajaService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class CajaServiceTest {

    @Autowired
    private lateinit var cajaService: CajaService

    @Autowired
    private lateinit var cajaRepository: CajaRepository

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Test
    fun `abre una caja con monto inicial y estado abierta`() {
        val usuario = crearUsuario("caja-vendedor")

        val caja = cajaService.abrirCaja(usuario.idUsuario, BigDecimal("50000.00"))

        assertEquals(BigDecimal("50000.00"), caja.montoInicial)
        assertEquals("ABIERTA", caja.estado)
        assertNull(caja.fechaCierre)
    }

    @Test
    fun `no permite abrir una segunda caja mientras haya una abierta`() {
        val usuario = crearUsuario("caja-vendedor-2")
        cajaService.abrirCaja(usuario.idUsuario, BigDecimal("10000.00"))

        assertFailsWith<OperacionNoPermitidaException> {
            cajaService.abrirCaja(usuario.idUsuario, BigDecimal("20000.00"))
        }

        assertEquals(1, cajaRepository.findAll().count { it.estado == "ABIERTA" })
    }

    @Test
    fun `cierra la caja abierta con monto final`() {
        val usuario = crearUsuario("caja-vendedor-3")
        val caja = cajaService.abrirCaja(usuario.idUsuario, BigDecimal("10000.00"))

        val cerrada = cajaService.cerrarCaja(usuario.idUsuario, BigDecimal("15000.00"))

        assertEquals("CERRADA", cerrada.estado)
        assertEquals(BigDecimal("15000.00"), cerrada.montoFinal)
        assertTrue(cerrada.fechaCierre != null)
    }

    @Test
    fun `rechaza cerrar una caja cuando no hay ninguna abierta`() {
        val usuario = crearUsuario("caja-vendedor-4")

        assertFailsWith<ReglaNegocioException> {
            cajaService.cerrarCaja(usuario.idUsuario, BigDecimal("1000.00"))
        }
    }

    @Test
    fun `estado devuelve la caja abierta o null`() {
        val usuario = crearUsuario("caja-vendedor-5")

        assertNull(cajaService.estadoCaja())

        cajaService.abrirCaja(usuario.idUsuario, BigDecimal("1000.00"))

        val estado = cajaService.estadoCaja()
        assertTrue(estado != null)
        assertEquals("ABIERTA", estado!!.estado)
    }

    private fun crearUsuario(nombre: String): Usuario =
        usuarioRepository.save(Usuario(usuario = nombre, password = "secreto", rolUsuario = "VENDEDOR"))
}

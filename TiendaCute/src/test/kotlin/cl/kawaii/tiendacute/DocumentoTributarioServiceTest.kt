package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.exception.OperacionNoPermitidaException
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.DocumentoTributario
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.model.Venta
import cl.kawaii.tiendacute.repository.DocumentoTributarioRepository
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.repository.VentaRepository
import cl.kawaii.tiendacute.service.DocumentoTributarioService
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
class DocumentoTributarioServiceTest {

    @Autowired
    private lateinit var documentoService: DocumentoTributarioService

    @Autowired
    private lateinit var documentoRepository: DocumentoTributarioRepository

    @Autowired
    private lateinit var ventaRepository: VentaRepository

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Test
    fun `emite un documento sobre una venta pagada completa`() {
        val usuario = crearUsuario("doc-vendedor")
        val venta = crearVentaPagada("doc-venta-1", usuario, BigDecimal("1000.00"))

        val documento = documentoService.emitirDocumento(venta.idVenta, "BOLETA")

        assertEquals("BOLETA", documento.tipo)
        assertEquals(1, documento.folio)
        assertEquals("EMITIDO", documento.estadoSii)
        assertTrue(documento.fechaEnvio != null)
        val xml = documento.xml
        assertTrue(xml != null && xml.isNotEmpty())
        assertEquals(venta.idVenta, documento.venta.idVenta)
    }

    @Test
    fun `rechaza emitir sobre una venta que no está pagada completa`() {
        val usuario = crearUsuario("doc-vendedor-2")
        // Venta con saldo pendiente (montoPagado < total).
        val venta = ventaRepository.save(
            Venta(
                tipoDocumento = "BOLETA",
                estadoVenta = "COMPLETADA",
                total = BigDecimal("1000.00"),
                montoPagado = BigDecimal("300.00"),
                usuario = usuario
            )
        )

        assertFailsWith<ReglaNegocioException> {
            documentoService.emitirDocumento(venta.idVenta, "BOLETA")
        }
    }

    @Test
    fun `rechaza emitir sobre una venta inexistente`() {
        val usuario = crearUsuario("doc-vendedor-3")

        assertFailsWith<RecursoNoEncontradoException> {
            documentoService.emitirDocumento(999_999, "BOLETA")
        }
    }

    @Test
    fun `no permite emitir dos documentos para la misma venta`() {
        val usuario = crearUsuario("doc-vendedor-4")
        val venta = crearVentaPagada("doc-venta-2", usuario, BigDecimal("500.00"))

        documentoService.emitirDocumento(venta.idVenta, "FACTURA")

        assertFailsWith<OperacionNoPermitidaException> {
            documentoService.emitirDocumento(venta.idVenta, "BOLETA")
        }
    }

    @Test
    fun `el folio es correlativo por tipo de documento`() {
        val usuario = crearUsuario("doc-vendedor-5")
        val venta1 = crearVentaPagada("doc-venta-3", usuario, BigDecimal("100.00"))
        val venta2 = crearVentaPagada("doc-venta-4", usuario, BigDecimal("200.00"))

        val d1 = documentoService.emitirDocumento(venta1.idVenta, "BOLETA")
        val d2 = documentoService.emitirDocumento(venta2.idVenta, "BOLETA")

        assertEquals(1, d1.folio)
        assertEquals(2, d2.folio)
    }

    private fun crearUsuario(nombre: String): Usuario =
        usuarioRepository.save(Usuario(usuario = nombre, password = "secreto", rolUsuario = "VENDEDOR"))

    private fun crearVentaPagada(codigo: String, usuario: Usuario, total: BigDecimal): Venta =
        ventaRepository.save(
            Venta(
                tipoDocumento = "BOLETA",
                estadoVenta = "COMPLETADA",
                total = total,
                montoPagado = total,
                usuario = usuario
            )
        )
}

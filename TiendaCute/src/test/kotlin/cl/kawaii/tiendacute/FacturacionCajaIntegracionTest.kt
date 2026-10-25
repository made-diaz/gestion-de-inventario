package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.dto.PagoRequest
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.model.Venta
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.repository.VentaRepository
import cl.kawaii.tiendacute.security.JwtService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@AutoConfigureMockMvc
class FacturacionCajaIntegracionTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var ventaRepository: VentaRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    private lateinit var token: String

    @Test
    fun `abre y cierra caja, y rechaza una segunda apertura mientras está abierta`() {
        val usuario = crearUsuario("caja-int", "ADMIN")
        token = jwtService.generarToken(usuario)

        val abrir1 = mockMvc.post("/api/cajas/abrir") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuarioId":${usuario.idUsuario},"montoInicial":50000}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString

        val idCaja = objectMapper.readTree(abrir1).get("idCaja").asLong()

        // Segunda apertura mientras hay una abierta → 409.
        mockMvc.post("/api/cajas/abrir") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuarioId":${usuario.idUsuario},"montoInicial":20000}"""
        }.andExpect { status { isConflict() } }

        // Estado: caja abierta.
        val estado = mockMvc.get("/api/cajas/estado") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        assertEquals(idCaja, objectMapper.readTree(estado).get("idCaja").asLong())

        // Cierre.
        mockMvc.post("/api/cajas/cerrar") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuarioId":${usuario.idUsuario},"montoFinal":52000}"""
        }.andExpect { status { isOk() } }

        // Ahora sí se puede abrir otra.
        mockMvc.post("/api/cajas/abrir") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuarioId":${usuario.idUsuario},"montoInicial":10000}"""
        }.andExpect { status { isCreated() } }
    }

    @Test
    fun `rechaza un pago que excede el saldo pendiente de la venta`() {
        val usuario = crearUsuario("pago-int", "VENDEDOR")
        token = jwtService.generarToken(usuario)

        val venta = ventaRepository.save(
            Venta(tipoDocumento = "BOLETA", estadoVenta = "COMPLETADA", total = BigDecimal("1000.00"), montoPagado = BigDecimal.ZERO, usuario = usuario)
        )

        mockMvc.post("/api/pagos") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"ventaId":${venta.idVenta},"usuarioId":${usuario.idUsuario},"metodoPago":"EFECTIVO","montoPago":1500}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.estado") { value(400) }
        }
    }

    @Test
    fun `registra un pago válido y reduce el saldo pendiente`() {
        val usuario = crearUsuario("pago-int-2", "VENDEDOR")
        token = jwtService.generarToken(usuario)

        val venta = ventaRepository.save(
            Venta(tipoDocumento = "BOLETA", estadoVenta = "COMPLETADA", total = BigDecimal("1000.00"), montoPagado = BigDecimal.ZERO, usuario = usuario)
        )

        mockMvc.post("/api/pagos") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"ventaId":${venta.idVenta},"usuarioId":${usuario.idUsuario},"metodoPago":"TARJETA","montoPago":400}"""
        }.andExpect { status { isCreated() } }

        val despues = ventaRepository.findById(venta.idVenta).orElseThrow()
        assertEquals(BigDecimal("600.00"), despues.saldoPendiente())
    }

    @Test
    fun `emite un documento solo sobre una venta pagada completa`() {
        val usuario = crearUsuario("doc-int", "VENDEDOR")
        token = jwtService.generarToken(usuario)

        val ventaPaga = ventaRepository.save(
            Venta(tipoDocumento = "BOLETA", estadoVenta = "COMPLETADA", total = BigDecimal("500.00"), montoPagado = BigDecimal("500.00"), usuario = usuario)
        )
        val ventaSinPagar = ventaRepository.save(
            Venta(tipoDocumento = "BOLETA", estadoVenta = "COMPLETADA", total = BigDecimal("300.00"), montoPagado = BigDecimal.ZERO, usuario = usuario)
        )

        // Venta pagada → 201.
        mockMvc.post("/api/documentos-tributarios/emitir") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"ventaId":${ventaPaga.idVenta},"tipo":"FACTURA"}"""
        }.andExpect { status { isCreated() } }

        // Venta no pagada → 400.
        mockMvc.post("/api/documentos-tributarios/emitir") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"ventaId":${ventaSinPagar.idVenta},"tipo":"BOLETA"}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.estado") { value(400) }
        }

        // Doble emisión sobre la misma venta → 409.
        mockMvc.post("/api/documentos-tributarios/emitir") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"ventaId":${ventaPaga.idVenta},"tipo":"BOLETA"}"""
        }.andExpect {
            status { isConflict() }
            jsonPath("$.estado") { value(409) }
        }
    }

    private fun crearUsuario(baseNombre: String, rol: String): Usuario {
        val nombre = "$baseNombre-${System.nanoTime()}"
        return usuarioRepository.save(
            Usuario(
                usuario = nombre,
                password = passwordEncoder.encode("clave-de-prueba"),
                rolUsuario = rol,
                activoUsuario = true
            )
        )
    }
}

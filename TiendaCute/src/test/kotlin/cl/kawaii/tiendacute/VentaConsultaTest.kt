package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.security.JwtService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Consulta de ventas: sin esto la API registraba ventas que nadie podía volver
 * a leer.
 */
@SpringBootTest
@AutoConfigureMockMvc
class VentaConsultaTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var token: String

    @BeforeEach
    fun preparar() {
        val usuario = usuarioRepository.findByUsuario("consulta-ventas")
            .orElseGet { crearUsuario() }
        token = jwtService.generarToken(usuario)
    }

    @Test
    fun `la venta registrada se puede consultar por id`() {
        val productoId = crearProducto("CONSULTA-ID")
        val ventaId = registrarVenta(productoId, 3)

        val cuerpo = mockMvc.get("/api/ventas/$ventaId") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString

        val nodo = objectMapper.readTree(cuerpo)
        assertEquals(ventaId, nodo.get("idVenta").asLong())
        assertEquals("3000.00", nodo.get("totalNeto").decimalValue().setScale(2).toPlainString())
        assertEquals("3570.00", nodo.get("total").decimalValue().setScale(2).toPlainString())
        assertEquals("BOLETA", nodo.get("tipoDocumento").asText())
    }

    @Test
    fun `listar ventas devuelve la venta registrada`() {
        val productoId = crearProducto("CONSULTA-LISTA")
        val ventaId = registrarVenta(productoId, 1)

        val cuerpo = mockMvc.get("/api/ventas") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString
        println("CUERPO_LISTAR_VENTAS: $cuerpo")

        val nodo = objectMapper.readTree(cuerpo)
        val ids = if (nodo.isArray) {
            nodo.map { it.get("idVenta").asLong() }
        } else {
            nodo.get("content").map { it.get("idVenta").asLong() }
        }
        assertTrue(ids.contains(ventaId), "El listado debía incluir la venta $ventaId, contenía $ids")
    }

    @Test
    fun `consultar una venta inexistente devuelve 404`() {
        mockMvc.get("/api/ventas/999999") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.estado") { value(404) }
        }
    }

    private fun crearUsuario(): Usuario =
        usuarioRepository.save(
            Usuario(
                usuario = "consulta-ventas",
                password = passwordEncoder.encode("clave-de-prueba"),
                rolUsuario = "ADMIN",
                activoUsuario = true
            )
        )

    private fun crearProducto(codigo: String): Long {
        val categoria = mockMvc.post("/api/categorias") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"nombreCategoria":"Cat $codigo"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString

        val categoriaId = objectMapper.readTree(categoria).get("idCategoria").asLong()

        val producto = mockMvc.post("/api/productos") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"nombreProducto":"Producto $codigo",
                 "codigoProducto":"$codigo",
                 "precioCompraProducto":500,
                 "precioVentaProducto":1000,
                 "stockActualProducto":10,
                 "categoria":{"idCategoria":$categoriaId}}
            """.trimIndent()
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString

        return objectMapper.readTree(producto).get("idProducto").asLong()
    }

    private fun registrarVenta(productoId: Long, cantidad: Int): Long {
        val usuarioId = usuarioRepository.findByUsuario("consulta-ventas").orElseThrow().idUsuario

        val cuerpo = mockMvc.post("/api/ventas") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"venta":{"tipoDocumento":"BOLETA"},
                 "detalles":[{"productoId":$productoId,"cantidad":$cantidad}],
                 "usuarioId":$usuarioId}
            """.trimIndent()
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString

        val ventaId = objectMapper.readTree(cuerpo).get("idVenta").asLong()
        println("VENTA_ID_CREADO: $ventaId")
        return ventaId
    }
}

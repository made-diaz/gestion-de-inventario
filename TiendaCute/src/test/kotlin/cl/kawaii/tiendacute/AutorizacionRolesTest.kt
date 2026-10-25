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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import kotlin.test.assertTrue

/**
 * La matriz de roles, probada por donde duele: el mostrador tiene que poder
 * vender sin poder tocar la administración.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutorizacionRolesTest {

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

    private lateinit var tokenAdmin: String
    private lateinit var tokenVendedor: String
    private var idAdmin: Long = 0

    @BeforeEach
    fun preparar() {
        // Idempotente: un usuario con ventas no se puede borrar (FK), así que se reutiliza.
        val admin = usuarioRepository.findByUsuario("roles-admin")
            .orElseGet { crearUsuario("roles-admin", "ADMIN") }
        val vendedor = usuarioRepository.findByUsuario("roles-vendedor")
            .orElseGet { crearUsuario("roles-vendedor", "VENDEDOR") }

        idAdmin = admin.idUsuario!!
        tokenAdmin = jwtService.generarToken(admin)
        tokenVendedor = jwtService.generarToken(vendedor)
    }

    @Test
    fun `un vendedor puede consultar el catalogo`() {
        mockMvc.get("/api/productos") {
            header("Authorization", "Bearer $tokenVendedor")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `un vendedor no puede crear productos`() {
        val categoriaId = crearCategoria("cat-vendedor-prohibido")

        mockMvc.post("/api/productos") {
            header("Authorization", "Bearer $tokenVendedor")
            contentType = MediaType.APPLICATION_JSON
            content = cuerpoProducto("VEND-PROHIBIDO", categoriaId)
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.estado") { value(403) }
        }
    }

    @Test
    fun `un vendedor puede listar ventas`() {
        mockMvc.get("/api/ventas") {
            header("Authorization", "Bearer $tokenVendedor")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `un vendedor no puede listar usuarios`() {
        mockMvc.get("/api/usuarios") {
            header("Authorization", "Bearer $tokenVendedor")
        }.andExpect { status { isForbidden() } }
    }

    @Test
    fun `un vendedor no puede eliminar usuarios`() {
        mockMvc.delete("/api/usuarios/$idAdmin") {
            header("Authorization", "Bearer $tokenVendedor")
        }.andExpect { status { isForbidden() } }

        assertTrue(
            usuarioRepository.existsById(idAdmin),
            "El usuario administrador no debía borrarse"
        )
    }

    @Test
    fun `un admin si puede crear productos`() {
        val categoriaId = crearCategoria("cat-admin-permitido")

        mockMvc.post("/api/productos") {
            header("Authorization", "Bearer $tokenAdmin")
            contentType = MediaType.APPLICATION_JSON
            content = cuerpoProducto("ADMIN-OK", categoriaId)
        }.andExpect { status { isCreated() } }
    }

    @Test
    fun `un vendedor puede registrar una venta`() {
        val categoriaId = crearCategoria("cat-venta-vendedor")
        val productoId = crearProducto("VENTA-VENDEDOR", categoriaId)

        mockMvc.post("/api/ventas") {
            header("Authorization", "Bearer $tokenVendedor")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"venta":{"tipoDocumento":"BOLETA"},
                 "detalles":[{"productoId":$productoId,"cantidad":2}],
                 "usuarioId":$idAdmin}
            """.trimIndent()
        }.andExpect { status { isCreated() } }

        mockMvc.get("/api/productos/$productoId") {
            header("Authorization", "Bearer $tokenVendedor")
        }.andExpect {
            status { isOk() }
            jsonPath("$.stockActualProducto") { value(8) }
        }
    }

    @Test
    fun `sin token la administracion responde 401 y no 403`() {
        mockMvc.get("/api/usuarios").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `borrar un usuario con ventas devuelve 409 y no 500`() {
        val usuario = crearUsuario("roles-con-historial", "VENDEDOR")
        val token = jwtService.generarToken(usuario)
        val categoriaId = crearCategoria("cat-historial")
        val productoId = crearProducto("HISTORIAL", categoriaId)

        mockMvc.post("/api/ventas") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"venta":{"tipoDocumento":"BOLETA"},
                 "detalles":[{"productoId":$productoId,"cantidad":1}],
                 "usuarioId":${usuario.idUsuario}}
            """.trimIndent()
        }.andExpect { status { isCreated() } }

        mockMvc.delete("/api/usuarios/${usuario.idUsuario}") {
            header("Authorization", "Bearer $tokenAdmin")
        }.andExpect {
            status { isConflict() }
            jsonPath("$.estado") { value(409) }
        }
    }

    private fun crearUsuario(nombre: String, rol: String): Usuario =
        usuarioRepository.save(
            Usuario(
                usuario = nombre,
                password = passwordEncoder.encode("clave-de-prueba"),
                rolUsuario = rol,
                activoUsuario = true
            )
        )

    private fun crearCategoria(nombre: String): Long {
        val cuerpo = mockMvc.post("/api/categorias") {
            header("Authorization", "Bearer $tokenAdmin")
            contentType = MediaType.APPLICATION_JSON
            content = """{"nombreCategoria":"$nombre"}"""
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString

        return objectMapper.readTree(cuerpo).get("idCategoria").asLong()
    }

    private fun crearProducto(codigo: String, categoriaId: Long): Long {
        val cuerpo = mockMvc.post("/api/productos") {
            header("Authorization", "Bearer $tokenAdmin")
            contentType = MediaType.APPLICATION_JSON
            content = cuerpoProducto(codigo, categoriaId)
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString

        return objectMapper.readTree(cuerpo).get("idProducto").asLong()
    }

    private fun cuerpoProducto(codigo: String, categoriaId: Long): String = """
        {"nombreProducto":"Producto $codigo",
         "codigoProducto":"$codigo",
         "precioCompraProducto":500,
         "precioVentaProducto":1000,
         "stockActualProducto":10,
         "categoria":{"idCategoria":$categoriaId}}
    """.trimIndent()
}

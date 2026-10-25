package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
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

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun prepararUsuarios() {
        listOf("auth-test", "auth-inactivo").forEach { nombre ->
            usuarioRepository.findByUsuario(nombre).ifPresent { usuarioRepository.delete(it) }
        }

        usuarioRepository.save(
            Usuario(
                usuario = "auth-test",
                password = passwordEncoder.encode("clave-correcta"),
                rolUsuario = "ADMIN"
            )
        )
        usuarioRepository.save(
            Usuario(
                usuario = "auth-inactivo",
                password = passwordEncoder.encode("clave-correcta"),
                rolUsuario = "VENDEDOR",
                activoUsuario = false
            )
        )
    }

    @Test
    fun `login valido devuelve token y habilita un endpoint protegido`() {
        val cuerpo = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"auth-test","password":"clave-correcta"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.tipo") { value("Bearer") }
            jsonPath("$.usuario") { value("auth-test") }
            jsonPath("$.rol") { value("ADMIN") }
            jsonPath("$.token") { isNotEmpty() }
        }.andReturn().response.contentAsString

        val token = objectMapper.readTree(cuerpo).get("token").asText()
        assertEquals(3, token.split(".").size, "El token debe tener formato JWT")
        val cabecera = String(java.util.Base64.getUrlDecoder().decode(token.split(".")[0]))
        assertTrue(cabecera.contains("\"alg\":\"HS256\""), "El token debe firmarse con HS256: $cabecera")

        mockMvc.get("/api/productos") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `login con password incorrecta devuelve 401`() {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"auth-test","password":"incorrecta"}"""
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.estado") { value(401) }
        }
    }

    @Test
    fun `login de usuario inactivo devuelve 403`() {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"auth-inactivo","password":"clave-correcta"}"""
        }.andExpect {
            status { isForbidden() }
            jsonPath("$.estado") { value(403) }
        }
    }

    @Test
    fun `endpoint protegido sin token devuelve 401`() {
        mockMvc.get("/api/productos").andExpect {
            status { isUnauthorized() }
            jsonPath("$.estado") { value(401) }
        }
    }

    @Test
    fun `endpoint protegido con token invalido devuelve 401`() {
        mockMvc.get("/api/productos") {
            header("Authorization", "Bearer token.invalido.firmado")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `login sin password devuelve 400`() {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"auth-test","password":""}"""
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.estado") { value(400) }
        }
    }

    @Test
    fun `la respuesta de login no expone el hash de la contrasena`() {
        val cuerpo = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"auth-test","password":"clave-correcta"}"""
        }.andExpect { status { isOk() } }.andReturn().response.contentAsString

        assertTrue(!cuerpo.contains("password"), "La respuesta no debe incluir la contraseña ni su hash")
    }
}

package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.security.JwtService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

/**
 * Endurecimiento del login: fuerza bruta, enumeración de usuarios y política
 * de contraseñas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadAutenticacionTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    @Test
    fun `tras cinco intentos fallidos el login responde 429`() {
        crearUsuario("bloqueo-integracion", "Clave1234")

        repeat(5) {
            intentoFallido("bloqueo-integracion").andExpect { status { isUnauthorized() } }
        }

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"bloqueo-integracion","password":"otra-mala"}"""
        }.andExpect {
            status { isTooManyRequests() }
            header { string("Retry-After", "900") }
            jsonPath("$.estado") { value(429) }
        }
    }

    @Test
    fun `un login exitoso reinicia el contador de fallos`() {
        crearUsuario("reseteo-integracion", "Clave1234")

        repeat(4) { intentoFallido("reseteo-integracion").andExpect { status { isUnauthorized() } } }

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"reseteo-integracion","password":"Clave1234"}"""
        }.andExpect { status { isOk() } }

        repeat(4) { intentoFallido("reseteo-integracion").andExpect { status { isUnauthorized() } } }
    }

    @Test
    fun `un usuario deshabilitado responde igual que una contrasena incorrecta`() {
        crearUsuario("deshabilitado-integracion", "Clave1234", activo = false)
        crearUsuario("comparacion-integracion", "Clave1234")

        val deshabilitado = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"deshabilitado-integracion","password":"Clave1234"}"""
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.estado") { value(401) }
        }.andReturn().response.contentAsString

        val malaClave = intentoFallido("comparacion-integracion").andReturn().response.contentAsString

        kotlin.test.assertEquals(
            malaClave, deshabilitado,
            "Las dos respuestas deben ser idénticas: si difieren, se puede enumerar usuarios"
        )
    }

    @Test
    fun `rechaza contrasenas debiles al crear usuarios`() {
        val admin = crearUsuario("admin-seguridad", "Clave1234", rol = "ADMIN")
        val token = jwtService.generarToken(admin)

        val invalidas = listOf(
            """{"usuario":"corta","password":"Ab1","rolUsuario":"VENDEDOR"}""",
            """{"usuario":"sinnumero","password":"solamenteletras","rolUsuario":"VENDEDOR"}""",
            """{"usuario":"abc12345","password":"abc12345","rolUsuario":"VENDEDOR"}"""
        )

        invalidas.forEach { cuerpo ->
            mockMvc.post("/api/usuarios") {
                header("Authorization", "Bearer $token")
                contentType = MediaType.APPLICATION_JSON
                content = cuerpo
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.estado") { value(400) }
            }
        }

        mockMvc.post("/api/usuarios") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"usuario":"valida","password":"Clave1234","rolUsuario":"VENDEDOR"}"""
        }.andExpect { status { isCreated() } }
    }

    private fun crearUsuario(
        nombre: String,
        password: String,
        rol: String = "VENDEDOR",
        activo: Boolean = true
    ): Usuario =
        usuarioRepository.findByUsuario(nombre).orElseGet {
            usuarioRepository.save(
                Usuario(
                    usuario = nombre,
                    password = passwordEncoder.encode(password),
                    rolUsuario = rol,
                    activoUsuario = activo
                )
            )
        }

    private fun intentoFallido(nombre: String) = mockMvc.post("/api/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"usuario":"$nombre","password":"incorrecta"}"""
    }
}

package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.security.JwtService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant
import java.util.Date

/**
 * Rechazos del filtro JWT: ningún token inválido debe abrir un endpoint
 * protegido. Son los casos que el camino feliz no cubre.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JwtRechazoTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    @Value("\${app.jwt.secret}")
    private lateinit var secreto: String

    @BeforeEach
    fun limpiar() {
        NOMBRES.forEach { nombre ->
            usuarioRepository.findByUsuario(nombre).ifPresent { usuarioRepository.delete(it) }
        }
    }

    @Test
    fun `un token emitido antes de deshabilitar la cuenta deja de servir`() {
        val usuario = crearUsuario("rechazo-deshabilitado")
        val token = jwtService.generarToken(usuario)

        mockMvc.get("/api/productos") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        usuario.activoUsuario = false
        usuarioRepository.save(usuario)

        mockMvc.get("/api/productos") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `un token expirado devuelve 401`() {
        crearUsuario("rechazo-expirado")
        val token = firmar("rechazo-expirado", expiracion = Instant.now().minusSeconds(3600))

        mockMvc.get("/api/productos") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `un token firmado con otra clave devuelve 401`() {
        crearUsuario("rechazo-firma")
        val token = firmar(
            "rechazo-firma",
            clave = "otra-clave-de-firma-suficientemente-larga-para-hs256"
        )

        mockMvc.get("/api/productos") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `un token de un usuario eliminado devuelve 401`() {
        val usuario = crearUsuario("rechazo-eliminado")
        val token = jwtService.generarToken(usuario)
        usuarioRepository.delete(usuario)

        mockMvc.get("/api/productos") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `un token sin claim de expiracion devuelve 401`() {
        crearUsuario("rechazo-sin-exp")
        val token = firmar("rechazo-sin-exp", expiracion = null)

        mockMvc.get("/api/productos") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isUnauthorized() } }
    }

    private fun crearUsuario(nombre: String, activo: Boolean = true): Usuario =
        usuarioRepository.save(
            Usuario(
                usuario = nombre,
                password = passwordEncoder.encode("clave-de-prueba"),
                rolUsuario = "VENDEDOR",
                activoUsuario = activo
            )
        )

    private fun firmar(
        usuario: String,
        clave: String = secreto,
        expiracion: Instant? = Instant.now().plusSeconds(3600)
    ): String {
        val builder = Jwts.builder()
            .subject(usuario)
            .issuedAt(Date.from(Instant.now()))
        expiracion?.let { builder.expiration(Date.from(it)) }
        return builder
            .signWith(Keys.hmacShaKeyFor(clave.toByteArray(Charsets.UTF_8)), Jwts.SIG.HS256)
            .compact()
    }

    private companion object {
        val NOMBRES = listOf(
            "rechazo-deshabilitado",
            "rechazo-expirado",
            "rechazo-firma",
            "rechazo-eliminado",
            "rechazo-sin-exp"
        )
    }
}

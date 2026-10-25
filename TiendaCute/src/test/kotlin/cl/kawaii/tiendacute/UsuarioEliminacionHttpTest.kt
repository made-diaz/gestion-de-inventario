package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.security.JwtService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.transaction.annotation.Transactional

/**
 * El estado de "último administrador" se arma dentro de la transacción del test
 * y se revierte al terminar: no se pisan los datos de las otras pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioEliminacionHttpTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var jwtService: JwtService

    @Test
    fun `no se puede borrar el ultimo administrador activo`() {
        desactivarAdministradores()
        val unicoAdmin = crearUsuario("unico-admin-http", "ADMIN")
        val token = jwtService.generarToken(unicoAdmin)

        mockMvc.delete("/api/usuarios/${unicoAdmin.idUsuario}") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isConflict() }
            jsonPath("$.estado") { value(409) }
        }
    }

    @Test
    fun `se puede borrar un administrador cuando queda otro activo`() {
        val actor = crearUsuario("admin-actor-http", "ADMIN")
        val otro = crearUsuario("admin-otro-http", "ADMIN")
        val token = jwtService.generarToken(actor)

        mockMvc.delete("/api/usuarios/${otro.idUsuario}") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isNoContent() }
        }
    }

    @Test
    fun `borrar un id inexistente sigue respondiendo 204`() {
        val actor = crearUsuario("admin-borrado-http", "ADMIN")
        val token = jwtService.generarToken(actor)

        mockMvc.delete("/api/usuarios/999999") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isNoContent() }
        }
    }

    private fun desactivarAdministradores() {
        usuarioRepository.findAll()
            .filter { it.rolUsuario.equals("ADMIN", ignoreCase = true) && it.activoUsuario }
            .forEach {
                it.activoUsuario = false
                usuarioRepository.save(it)
            }
    }

    private fun crearUsuario(nombre: String, rol: String): Usuario =
        usuarioRepository.save(
            Usuario(
                usuario = nombre,
                password = passwordEncoder.encode("Clave1234"),
                rolUsuario = rol,
                activoUsuario = true
            )
        )
}

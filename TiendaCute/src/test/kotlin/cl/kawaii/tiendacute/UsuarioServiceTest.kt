package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.service.UsuarioService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
class UsuarioServiceTest {

    @Autowired
    private lateinit var usuarioService: UsuarioService

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Test
    fun `guardar usuario almacena la contrasena hasheada`() {
        val guardado = usuarioService.guardarUsuario(
            Usuario(usuario = "hash-test", password = "secreto-plano1", rolUsuario = "ADMIN")
        )

        val enBase = usuarioRepository.findById(guardado.idUsuario).orElseThrow()

        assertFalse(enBase.password == "secreto-plano", "La contraseña no debe guardarse en texto plano")
        assertTrue(passwordEncoder.matches("secreto-plano1", enBase.password))
    }

    @Test
    fun `guardar usuario ignora el id enviado por el cliente`() {
        val existente = usuarioService.guardarUsuario(
            Usuario(usuario = "id-existente", password = "secreto123", rolUsuario = "ADMIN")
        )

        val nuevo = usuarioService.guardarUsuario(
            Usuario(idUsuario = existente.idUsuario, usuario = "id-nuevo", password = "secreto123", rolUsuario = "ADMIN")
        )

        assertFalse(nuevo.idUsuario == existente.idUsuario, "No debe sobrescribir al usuario existente")
        assertTrue(usuarioRepository.findById(existente.idUsuario).orElseThrow().usuario == "id-existente")
    }
}

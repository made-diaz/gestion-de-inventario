package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.dto.CambiarContrasenaRequest
import cl.kawaii.tiendacute.dto.UsuarioActualizarRequest
import cl.kawaii.tiendacute.exception.OperacionNoPermitidaException
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.service.UsuarioService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import org.junit.jupiter.api.BeforeEach
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest
@Transactional
class UsuarioActualizarServiceTest {

    @Autowired
    private lateinit var usuarioService: UsuarioService

    @Autowired
    private lateinit var usuarioRepository: UsuarioRepository

    @BeforeEach
    fun limpiarUsuarios() {
        usuarioRepository.deleteAll()
    }

    @Test
    fun `actualizar usuario normal cambia campos`() {
        val usuario = crearUsuario("upd-normal", "VENDEDOR", activo = true)

        val actualizado = usuarioService.actualizarUsuario(
            usuario.idUsuario,
            UsuarioActualizarRequest(usuario = "upd-cambiado", rolUsuario = "VENDEDOR", activoUsuario = true),
            usuarioIdQueHaceCambio = usuario.idUsuario
        )

        assertEquals("upd-cambiado", actualizado.usuario)
        assertEquals("VENDEDOR", actualizado.rolUsuario)
        assertTrue(actualizado.activoUsuario)
    }

    @Test
    fun `no se puede deshabilitar el ultimo admin activo`() {
        val admin = crearUsuario("upd-admin-unico", "ADMIN", activo = true)

        assertFailsWith<OperacionNoPermitidaException> {
            usuarioService.actualizarUsuario(
                admin.idUsuario,
                UsuarioActualizarRequest(activoUsuario = false),
                usuarioIdQueHaceCambio = admin.idUsuario
            )
        }

        val despues = usuarioRepository.findById(admin.idUsuario).orElseThrow()
        assertTrue(despues.activoUsuario) // no cambió
    }

    @Test
    fun `no se puede quitar rol admin al ultimo admin activo`() {
        val admin = crearUsuario("upd-admin-rol", "ADMIN", activo = true)

        assertFailsWith<OperacionNoPermitidaException> {
            usuarioService.actualizarUsuario(
                admin.idUsuario,
                UsuarioActualizarRequest(rolUsuario = "VENDEDOR"),
                usuarioIdQueHaceCambio = admin.idUsuario
            )
        }

        val despues = usuarioRepository.findById(admin.idUsuario).orElseThrow()
        assertEquals("ADMIN", despues.rolUsuario)
    }

    @Test
    fun `se puede deshabilitar admin cuando hay otro activo`() {
        val admin1 = crearUsuario("upd-admin-a", "ADMIN", activo = true)
        val admin2 = crearUsuario("upd-admin-b", "ADMIN", activo = true)

        val actualizado = usuarioService.actualizarUsuario(
            admin1.idUsuario,
            UsuarioActualizarRequest(activoUsuario = false),
            usuarioIdQueHaceCambio = admin2.idUsuario
        )

        assertFalse(actualizado.activoUsuario)
        assertTrue(usuarioRepository.findById(admin2.idUsuario).orElseThrow().activoUsuario)
    }

    @Test
    fun `usuario cambia su propia contraseña`() {
        val usuario = crearUsuario("upd-pass-propio", "VENDEDOR", activo = true)

        val actualizado = usuarioService.actualizarContraseña(
            usuario.idUsuario,
            CambiarContrasenaRequest("NuevaClave123"),
            usuarioIdQueHaceCambio = usuario.idUsuario
        )

        assertEquals(usuario.idUsuario, actualizado.idUsuario)
    }

    @Test
    fun `admin cambia contraseña de otro usuario`() {
        val admin = crearUsuario("upd-admin-pass", "ADMIN", activo = true)
        val objetivo = crearUsuario("upd-objetivo-pass", "VENDEDOR", activo = true)

        usuarioService.actualizarContraseña(
            objetivo.idUsuario,
            CambiarContrasenaRequest("ClaveAdmin456"),
            usuarioIdQueHaceCambio = admin.idUsuario
        )

        val despues = usuarioRepository.findById(objetivo.idUsuario).orElseThrow()
        assertEquals(objetivo.idUsuario, despues.idUsuario)
    }

    @Test
    fun `usuario no admin no puede cambiar contraseña de otro`() {
        val solicitante = crearUsuario("upd-solicitante", "VENDEDOR", activo = true)
        val objetivo = crearUsuario("upd-objetivo-noadmin", "VENDEDOR", activo = true)

        assertFailsWith<OperacionNoPermitidaException> {
            usuarioService.actualizarContraseña(
                objetivo.idUsuario,
                CambiarContrasenaRequest("ClaveCualquiera123"),
                usuarioIdQueHaceCambio = solicitante.idUsuario
            )
        }
    }

    @Test
    fun `cambiar contraseña con password invalida falla`() {
        val usuario = crearUsuario("upd-pass-invalida", "VENDEDOR", activo = true)

        assertFailsWith<ReglaNegocioException> {
            usuarioService.actualizarContraseña(
                usuario.idUsuario,
                CambiarContrasenaRequest("123"),
                usuarioIdQueHaceCambio = usuario.idUsuario
            )
        }
    }

    private fun crearUsuario(nombre: String, rol: String, activo: Boolean): Usuario =
        usuarioRepository.save(Usuario(usuario = nombre, password = "secreto", rolUsuario = rol, activoUsuario = activo))
}

package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.exception.OperacionNoPermitidaException
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.service.UsuarioService
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import java.util.Optional
import kotlin.test.assertFailsWith

/**
 * La guarda del último administrador se prueba sin base: lo que importa es la
 * decisión, no el motor de persistencia.
 */
class UsuarioEliminacionTest {

    private val repositorio = mock(UsuarioRepository::class.java)
    private val encoder = mock(PasswordEncoder::class.java)
    private val gestor = mock(PlatformTransactionManager::class.java)

    private val servicio: UsuarioService

    init {
        `when`(gestor.getTransaction(any(TransactionDefinition::class.java)))
            .thenReturn(mock(TransactionStatus::class.java))
        servicio = UsuarioService(repositorio, encoder, gestor)
    }

    @Test
    fun `no elimina el ultimo administrador activo`() {
        val admin = admin(1L)
        `when`(repositorio.findById(1L)).thenReturn(Optional.of(admin))
        `when`(repositorio.countByRolUsuarioAndActivoUsuario("ADMIN", true)).thenReturn(1L)

        assertFailsWith<OperacionNoPermitidaException> { servicio.eliminarUsuario(1L) }
        verify(repositorio, never()).delete(any(Usuario::class.java))
    }

    @Test
    fun `elimina un administrador cuando queda otro activo`() {
        val admin = admin(1L)
        `when`(repositorio.findById(1L)).thenReturn(Optional.of(admin))
        `when`(repositorio.countByRolUsuarioAndActivoUsuario("ADMIN", true)).thenReturn(2L)

        servicio.eliminarUsuario(1L)

        verify(repositorio).delete(admin)
    }

    @Test
    fun `elimina un vendedor sin consultar administradores`() {
        val vendedor = Usuario(
            idUsuario = 2L,
            usuario = "vendedor",
            password = "hash",
            rolUsuario = "VENDEDOR",
            activoUsuario = true
        )
        `when`(repositorio.findById(2L)).thenReturn(Optional.of(vendedor))

        servicio.eliminarUsuario(2L)

        verify(repositorio).delete(vendedor)
        verify(repositorio, never()).countByRolUsuarioAndActivoUsuario("ADMIN", true)
    }

    @Test
    fun `un administrador inactivo no bloquea la eliminacion`() {
        val adminInactivo = admin(3L).apply { activoUsuario = false }
        `when`(repositorio.findById(3L)).thenReturn(Optional.of(adminInactivo))

        servicio.eliminarUsuario(3L)

        verify(repositorio).delete(adminInactivo)
    }

    @Test
    fun `borrar un id inexistente no hace nada`() {
        `when`(repositorio.findById(99L)).thenReturn(Optional.empty())

        servicio.eliminarUsuario(99L)

        verify(repositorio, never()).delete(any(Usuario::class.java))
    }

    private fun admin(id: Long) = Usuario(
        idUsuario = id,
        usuario = "admin$id",
        password = "hash",
        rolUsuario = "ADMIN",
        activoUsuario = true
    )
}

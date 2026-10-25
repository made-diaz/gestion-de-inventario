package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.config.UsuarioInicialInitializer
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.boot.ApplicationArguments
import org.springframework.security.crypto.password.PasswordEncoder
import kotlin.test.assertFailsWith

/**
 * Un ADMIN_PASSWORD débil no puede dejar la aplicación arriba sin ningún
 * usuario: el operador se enteraría recién cuando nadie pueda entrar.
 */
class UsuarioInicialInitializerTest {

    private val repositorio = mock(UsuarioRepository::class.java)
    private val encoder = mock(PasswordEncoder::class.java)
    private val argumentos = mock(ApplicationArguments::class.java)

    @Test
    fun `una contrasena debil impide arrancar`() {
        `when`(repositorio.count()).thenReturn(0L)

        val inicializador = UsuarioInicialInitializer(repositorio, encoder, "admin", "123")

        assertFailsWith<IllegalStateException> { inicializador.run(argumentos) }
        verify(repositorio, never()).save(any(Usuario::class.java))
    }

    @Test
    fun `una contrasena sin numeros impide arrancar`() {
        `when`(repositorio.count()).thenReturn(0L)

        val inicializador = UsuarioInicialInitializer(repositorio, encoder, "admin", "sololetras")

        assertFailsWith<IllegalStateException> { inicializador.run(argumentos) }
    }

    @Test
    fun `una base con usuarios no toca nada`() {
        `when`(repositorio.count()).thenReturn(1L)

        val inicializador = UsuarioInicialInitializer(repositorio, encoder, "admin", "123")

        inicializador.run(argumentos)

        verify(repositorio, never()).save(any(Usuario::class.java))
    }
}

package cl.kawaii.tiendacute.security

import cl.kawaii.tiendacute.repository.UsuarioRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * Adapta la entidad Usuario al contrato de Spring Security.
 * `roles()` agrega el prefijo ROLE_ (por ejemplo rolUsuario "ADMIN" -> ROLE_ADMIN).
 */
@Service
class UsuarioDetailsService(private val usuarioRepository: UsuarioRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val usuario = usuarioRepository.findByUsuario(username)
            .orElseThrow { UsernameNotFoundException("Usuario no encontrado: $username") }

        return User.withUsername(usuario.usuario)
            .password(usuario.password)
            .roles(usuario.rolUsuario)
            .disabled(!usuario.activoUsuario)
            .build()
    }
}

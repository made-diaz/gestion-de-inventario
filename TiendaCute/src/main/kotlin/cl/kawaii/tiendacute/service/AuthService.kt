package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.LoginRequest
import cl.kawaii.tiendacute.dto.LoginResponse
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.security.JwtService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val usuarioRepository: UsuarioRepository,
    private val jwtService: JwtService
) {

    /**
     * Verifica las credenciales a través de Spring Security (BCrypt incluido)
     * y emite un JWT para el usuario autenticado.
     */
    fun login(request: LoginRequest): LoginResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.usuario, request.password)
        )

        val usuario = usuarioRepository.findByUsuario(request.usuario)
            .orElseThrow {
                RecursoNoEncontradoException("Usuario no encontrado: ${request.usuario}")
            }

        return LoginResponse(
            token = jwtService.generarToken(usuario),
            expiraEnSegundos = jwtService.expiracionEnSegundos(),
            usuario = usuario.usuario,
            rol = usuario.rolUsuario
        )
    }
}

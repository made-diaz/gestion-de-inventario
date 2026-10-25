package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.LoginRequest
import cl.kawaii.tiendacute.dto.LoginResponse
import cl.kawaii.tiendacute.exception.DemasiadosIntentosException
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.security.IntentoLoginService
import cl.kawaii.tiendacute.security.JwtService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val usuarioRepository: UsuarioRepository,
    private val jwtService: JwtService,
    private val intentoLoginService: IntentoLoginService
) {

    /**
     * Verifica las credenciales a través de Spring Security (BCrypt incluido)
     * y emite un JWT para el usuario autenticado.
     *
     * Los fallos se cuentan por usuario + IP: así un atacante no puede dejar
     * afuera a un usuario legítimo adivinando su nombre desde otra máquina.
     */
    fun login(request: LoginRequest, ip: String): LoginResponse {
        val clave = "${request.usuario.lowercase()}|$ip"

        if (intentoLoginService.estaBloqueado(clave)) {
            throw DemasiadosIntentosException(
                intentoLoginService.bloqueoEnSegundos(),
                "Demasiados intentos fallidos. Esperá unos minutos antes de reintentar."
            )
        }

        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.usuario, request.password)
            )
        } catch (e: AuthenticationException) {
            intentoLoginService.registrarFallo(clave)
            throw e
        }

        // Credenciales correctas: se borra el historial de fallos.
        intentoLoginService.limpiar(clave)

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

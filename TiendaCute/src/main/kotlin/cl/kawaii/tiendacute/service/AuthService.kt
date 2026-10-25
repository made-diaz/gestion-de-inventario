package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.LoginRequest
import cl.kawaii.tiendacute.dto.LoginResponse
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.security.IntentoLoginService
import cl.kawaii.tiendacute.security.JwtService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
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
     * Los intentos se cuentan por usuario + IP: así un atacante no puede dejar
     * afuera a un usuario legítimo adivinando su nombre desde otra máquina.
     *
     * El intento se reserva antes de verificar la contraseña, no después: si se
     * contara al fallar, una ráfaga de requests concurrentes pasaría entera por
     * el mismo hueco y el límite no serviría de nada.
     */
    fun login(request: LoginRequest, ip: String): LoginResponse {
        val clave = "${request.usuario.lowercase()}|$ip"

        // Lanza 429 si corresponde; consume un intento si no.
        intentoLoginService.reservarIntento(clave)

        // Si falla, el intento ya quedó contado al reservarlo.
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.usuario, request.password)
        )

        // Credenciales correctas: se borra el historial de intentos.
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

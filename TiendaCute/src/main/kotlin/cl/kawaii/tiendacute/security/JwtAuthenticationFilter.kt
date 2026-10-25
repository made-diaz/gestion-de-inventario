package cl.kawaii.tiendacute.security

import io.jsonwebtoken.JwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Lee el header Authorization, valida el JWT y deja la autenticación en el
 * SecurityContext. Si el token falta o es inválido no autentica: la cadena de
 * seguridad responde 401 con el entry point configurado.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService,
    private val usuarioDetailsService: UsuarioDetailsService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = leerToken(request)

        if (token != null && SecurityContextHolder.getContext().authentication == null) {
            try {
                val nombreUsuario = jwtService.extraerUsuario(token)

                if (jwtService.esValido(token, nombreUsuario)) {
                    val userDetails = usuarioDetailsService.loadUserByUsername(nombreUsuario)

                    // La cuenta pudo deshabilitarse después de emitir el token.
                    if (cuentaHabilitada(userDetails)) {
                        val autenticacion = UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.authorities
                        )
                        autenticacion.details = WebAuthenticationDetailsSource().buildDetails(request)
                        SecurityContextHolder.getContext().authentication = autenticacion
                    }
                }
            } catch (e: JwtException) {
                // Token inválido o expirado: se continúa sin autenticar.
            } catch (e: UsernameNotFoundException) {
                // El usuario del token ya no existe.
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun cuentaHabilitada(userDetails: UserDetails): Boolean =
        userDetails.isEnabled &&
            userDetails.isAccountNonLocked &&
            userDetails.isAccountNonExpired &&
            userDetails.isCredentialsNonExpired

    private fun leerToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        if (!header.startsWith(PREFIJO_BEARER, ignoreCase = true)) return null
        return header.substring(PREFIJO_BEARER.length).trim().ifEmpty { null }
    }

    private companion object {
        const val PREFIJO_BEARER = "Bearer "
    }
}

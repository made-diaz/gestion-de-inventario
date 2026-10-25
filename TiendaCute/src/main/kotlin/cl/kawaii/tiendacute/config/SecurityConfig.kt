package cl.kawaii.tiendacute.config

import cl.kawaii.tiendacute.exception.ErrorResponse
import cl.kawaii.tiendacute.security.JwtAuthenticationFilter
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val objectMapper: ObjectMapper
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            // Sin sesión: cada request se autentica con su propio token.
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                // El despacho de errores del contenedor no debe convertirse en 403.
                it.requestMatchers("/error").permitAll()

                // Catálogo y consultas: los dos roles.
                it.requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**", "/api/clientes/**")
                    .hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers(
                    HttpMethod.GET,
                    "/api/pagos/**",
                    "/api/documentos-tributarios/**",
                    "/api/movimientos-stock/**",
                    "/api/ventas/**"
                ).hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)

                // Operación de mostrador: vender, cobrar y registrar clientes.
                it.requestMatchers(HttpMethod.POST, "/api/ventas").hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers(HttpMethod.POST, "/api/pagos").hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers(HttpMethod.POST, "/api/cajas/**").hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers(HttpMethod.POST, "/api/documentos-tributarios/emitir").hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers(HttpMethod.POST, "/api/clientes").hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)

                // Administración: usuarios y todo lo que modifica el catálogo.
                it.requestMatchers("/api/usuarios/**").hasRole(ROL_ADMIN)
                it.anyRequest().hasRole(ROL_ADMIN)
            }
            .exceptionHandling {
                it.authenticationEntryPoint { _, response, _ ->
                    escribirError(response, HttpStatus.UNAUTHORIZED, "Token ausente, inválido o expirado")
                }
                it.accessDeniedHandler { _, response, _ ->
                    escribirError(response, HttpStatus.FORBIDDEN, "No tenés permisos para esta operación")
                }
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    /**
     * Las contraseñas se guardan con BCrypt, nunca en texto plano.
     * Las cuentas creadas antes de este cambio deben resetearse.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(configuracion: AuthenticationConfiguration): AuthenticationManager =
        configuracion.authenticationManager

    private fun escribirError(response: HttpServletResponse, estado: HttpStatus, mensaje: String) {
        response.status = estado.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        objectMapper.writeValue(response.writer, ErrorResponse(estado.value(), mensaje))
    }

    private companion object {
        // hasRole() agrega el prefijo ROLE_, así que acá van sin prefijo.
        const val ROL_ADMIN = "ADMIN"
        const val ROL_VENDEDOR = "VENDEDOR"
    }
}

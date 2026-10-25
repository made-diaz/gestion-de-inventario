package cl.kawaii.tiendacute.config

import cl.kawaii.tiendacute.exception.ErrorResponse
import cl.kawaii.tiendacute.security.JwtAuthenticationFilter
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
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
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val objectMapper: ObjectMapper,
    @Value("\${app.cors.allowed-origins:http://localhost:5173}") private val allowedOrigins: String
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .headers {
                it.contentTypeOptions { }
                it.frameOptions { it.deny() }
                it.referrerPolicy { it.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN) }
            }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS).permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                it.requestMatchers("/error").permitAll()
                it.requestMatchers(HttpMethod.GET, "/api/productos/**", "/api/categorias/**", "/api/clientes/**")
                    .hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers(
                    HttpMethod.GET,
                    "/api/pagos/**",
                    "/api/documentos-tributarios/**",
                    "/api/movimientos-stock/**",
                    "/api/ventas/**"
                ).hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers(HttpMethod.POST, "/api/ventas").hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers(HttpMethod.POST, "/api/pagos").hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers(HttpMethod.POST, "/api/cajas/**").hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers(HttpMethod.POST, "/api/documentos-tributarios/emitir").hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers(HttpMethod.POST, "/api/clientes").hasAnyRole(ROL_ADMIN, ROL_VENDEDOR)
                it.requestMatchers("/api/usuarios/**").hasRole(ROL_ADMIN)
                it.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
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

    // TODO: Reagregar CORS ConfigurationSource una vez resuelto el conflicto con MockMvc en tests.
    // El CORS se maneja temporalmente sin bean de Spring Security.

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val origins = allowedOrigins
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        val finalOrigins = if (origins.isEmpty()) listOf("http://localhost:5173") else origins

        val config = CorsConfiguration()
        config.allowedOrigins = finalOrigins
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("Authorization", "Content-Type")
        config.exposedHeaders = listOf("Content-Disposition", "X-Total-Count", "Retry-After")
        config.allowCredentials = true
        config.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/api/**", config)
        return source
    }

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
        const val ROL_ADMIN = "ADMIN"
        const val ROL_VENDEDOR = "VENDEDOR"
    }
}

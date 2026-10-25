package cl.kawaii.tiendacute.config

import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

/**
 * Crea el primer usuario ADMIN cuando la base está vacía.
 * Sin esto la API queda inaccesible: todos los endpoints exigen token y el
 * único endpoint público es el login.
 *
 * La contraseña se toma de ADMIN_PASSWORD (env.properties). Si no está
 * definida, no se crea nada y queda un aviso en el log: nunca hay una
 * contraseña por defecto cableada en el código.
 */
@Component
class UsuarioInicialInitializer(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${app.admin.usuario:admin}") private val adminUsuario: String,
    @Value("\${app.admin.password:}") private val adminPassword: String
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        if (usuarioRepository.count() > 0) return

        if (adminPassword.isBlank()) {
            log.warn(
                "No hay usuarios en la base y app.admin.password está vacío. " +
                    "Definí ADMIN_PASSWORD en env.properties para crear el usuario inicial."
            )
            return
        }

        usuarioRepository.save(
            Usuario(
                usuario = adminUsuario,
                password = passwordEncoder.encode(adminPassword),
                rolUsuario = "ADMIN"
            )
        )
        log.info("Usuario inicial '{}' creado con rol ADMIN. Cambiá la contraseña antes de publicar.", adminUsuario)
    }
}

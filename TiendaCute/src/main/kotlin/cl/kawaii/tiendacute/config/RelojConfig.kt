package cl.kawaii.tiendacute.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

/**
 * Reloj inyectable: los tests de expiración no dependen de esperar minutos
 * reales, pueden mover el tiempo a voluntad.
 */
@Configuration
class RelojConfig {

    @Bean
    fun reloj(): Clock = Clock.systemUTC()
}

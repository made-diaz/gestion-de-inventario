package cl.kawaii.tiendacute.config

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Sin este módulo, Jackson intenta serializar los proxies de Hibernate
 * (por ejemplo Producto.categoria) y falla con ByteBuddyInterceptor.
 *
 * FORCE_LAZY_LOADING resuelve las relaciones antes de escribir el JSON, así el
 * cliente recibe los datos completos en lugar de `null`. Requiere la sesión
 * abierta durante la serialización, que es el comportamiento por defecto
 * (spring.jpa.open-in-view=true).
 */
@Configuration
class JacksonConfig {

    @Bean
    fun hibernate6Module(): Module =
        Hibernate6Module().enable(Hibernate6Module.Feature.FORCE_LAZY_LOADING)
}

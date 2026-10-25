package cl.kawaii.tiendacute.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun tiendaCuteOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("TiendaCute API")
                    .version("1.0.0")
                    .description("API de inventario, ventas, caja y facturación para tiendas minoristas.")
            )
            .servers(
                listOf(
                    Server().url("/").description("Servidor actual")
                )
            )
            .addSecurityItem(
                SecurityRequirement().addList("bearerAuth")
            )
            .components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("Token JWT obtenido en POST /api/auth/login")
                    )
            )
    }
}

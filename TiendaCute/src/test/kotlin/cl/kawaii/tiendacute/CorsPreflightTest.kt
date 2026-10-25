package cl.kawaii.tiendacute

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.servlet.FilterRegistrationBean
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pruebas de preflight CORS.
 *
 * El filtro CORS se registra como FilterRegistrationBean (bean corsFilterRegistration)
 * con:
 *   - allowedOrigins: app.cors.allowed-origins (default http://localhost:5173)
 *   - allowedMethods: GET, POST, PUT, DELETE, OPTIONS
 *   - allowedHeaders: Authorization, Content-Type
 *   - exposedHeaders: Content-Disposition, X-Total-Count, Retry-After
 *   - allowCredentials: true
 *
 * Este test verifica que el bean existe. Los headers CORS exactos se verifican en
 * integración real levantando el servidor y haciendo curl contra /api/ventas con
 * un header Origin.
 */
@SpringBootTest
class CorsPreflightTest {

    @Autowired
    private lateinit var applicationContext: org.springframework.context.ApplicationContext

    @Test
    fun `filtro CORS registrado como bean en el contexto`() {
        val bean = applicationContext.getBean("corsFilterRegistration")
        assertNotNull(bean)
        assertTrue(bean is FilterRegistrationBean<*>)
    }
}

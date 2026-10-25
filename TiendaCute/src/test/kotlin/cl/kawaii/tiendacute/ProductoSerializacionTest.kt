package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.model.Categoria
import cl.kawaii.tiendacute.model.Producto
import cl.kawaii.tiendacute.repository.CategoriaRepository
import cl.kawaii.tiendacute.repository.ProductoRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.math.BigDecimal

/**
 * Regresión: al serializar una entidad con relaciones LAZY, Jackson debe
 * resolverlas con el módulo de Hibernate en lugar de fallar con un 500.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = ["ADMIN"])
class ProductoSerializacionTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var productoRepository: ProductoRepository

    @Autowired
    private lateinit var categoriaRepository: CategoriaRepository

    @Test
    fun `GET producto devuelve la categoria resuelta`() {
        val categoria = categoriaRepository.save(Categoria(nombreCategoria = "Categoría serialización"))
        val producto = productoRepository.save(
            Producto(
                nombreProducto = "Producto serialización",
                codigoProducto = "SKU-SERIALIZACION",
                precioCompraProducto = BigDecimal("100.00"),
                precioVentaProducto = BigDecimal("200.00"),
                stockActualProducto = 1,
                categoria = categoria
            )
        )

        mockMvc.get("/api/productos/{id}", producto.idProducto)
            .andExpect {
                status { isOk() }
                jsonPath("$.nombreProducto") { value("Producto serialización") }
                jsonPath("$.categoria.nombreCategoria") { value("Categoría serialización") }
            }
    }
}

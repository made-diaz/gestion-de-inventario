package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.dto.CategoriaActualizarRequest
import cl.kawaii.tiendacute.model.Categoria
import cl.kawaii.tiendacute.repository.CategoriaRepository
import cl.kawaii.tiendacute.service.CategoriaService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals

@SpringBootTest
class CategoriaActualizarServiceTest {

    @Autowired
    private lateinit var categoriaService: CategoriaService

    @Autowired
    private lateinit var categoriaRepository: CategoriaRepository

    @Test
    fun `actualizar categoria cambia nombre`() {
        val categoria = categoriaRepository.save(Categoria(nombreCategoria = "Vieja"))

        val actualizada = categoriaService.actualizarCategoria(
            categoria.idCategoria,
            CategoriaActualizarRequest(nombreCategoria = "Nueva categoría")
        )

        assertEquals("Nueva categoría", actualizada.nombreCategoria)
        assertEquals(categoria.idCategoria, actualizada.idCategoria)
    }

    @Test
    fun `actualizar categoria sin nombre no cambia nada`() {
        val categoria = categoriaRepository.save(Categoria(nombreCategoria = "Intacta"))

        categoriaService.actualizarCategoria(categoria.idCategoria, CategoriaActualizarRequest())

        val despues = categoriaRepository.findById(categoria.idCategoria).orElseThrow()
        assertEquals("Intacta", despues.nombreCategoria)
    }
}

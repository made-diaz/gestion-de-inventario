package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.CategoriaActualizarRequest
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.model.Categoria
import cl.kawaii.tiendacute.repository.CategoriaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoriaService(private val categoriaRepository: CategoriaRepository) {

    /**
     * Lista categorías con paginación opcional.
     */
    @Transactional(readOnly = true)
    fun listarCategorias(pageable: Pageable?): Page<Categoria> {
        return categoriaRepository.findAll(pageable ?: Pageable.unpaged())
    }

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Categoria = categoriaRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Categoría no encontrada con ID: $id") }

    @Transactional
    fun guardarCategoria(categoria: Categoria): Categoria {
        categoria.idCategoria = 0
        return categoriaRepository.save(categoria)
    }

    @Transactional
    fun actualizarCategoria(id: Long, request: CategoriaActualizarRequest): Categoria {
        val categoria = buscarPorId(id)
        if (request.nombreCategoria?.isNotBlank() == true) {
            categoria.nombreCategoria = request.nombreCategoria
        }
        return categoriaRepository.save(categoria)
    }

    @Transactional
    fun eliminarCategoria(id: Long) = categoriaRepository.deleteById(id)
}

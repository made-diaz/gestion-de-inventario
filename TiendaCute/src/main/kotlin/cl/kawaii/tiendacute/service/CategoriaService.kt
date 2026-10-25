package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.model.Categoria
import cl.kawaii.tiendacute.repository.CategoriaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CategoriaService(private val categoriaRepository: CategoriaRepository) {

    @Transactional(readOnly = true)
    fun listarCategorias(): List<Categoria> = categoriaRepository.findAll()

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Categoria = categoriaRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Categoría no encontrada con ID: $id") }

    @Transactional
    fun guardarCategoria(categoria: Categoria): Categoria {
        // Es un alta: se ignora cualquier id enviado por el cliente.
        categoria.idCategoria = 0
        return categoriaRepository.save(categoria)
    }

    @Transactional
    fun eliminarCategoria(id: Long) = categoriaRepository.deleteById(id)
}
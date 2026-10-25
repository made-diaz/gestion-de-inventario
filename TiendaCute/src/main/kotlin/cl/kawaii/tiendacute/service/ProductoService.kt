package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.model.Producto
import cl.kawaii.tiendacute.repository.ProductoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductoService(private val productoRepository: ProductoRepository) {

    @Transactional(readOnly = true)
    fun listarProductos(): List<Producto> = productoRepository.findAll()

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Producto = productoRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Producto no encontrado con ID: $id") }

    @Transactional
    fun guardarProducto(producto: Producto): Producto {
        // Es un alta: se ignora cualquier id enviado por el cliente.
        producto.idProducto = 0
        return productoRepository.save(producto)
    }

    @Transactional
    fun eliminarProducto(id: Long) = productoRepository.deleteById(id)
}
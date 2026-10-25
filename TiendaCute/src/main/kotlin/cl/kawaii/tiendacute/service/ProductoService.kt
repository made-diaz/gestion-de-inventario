package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.ProductoActualizarRequest
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.model.Categoria
import cl.kawaii.tiendacute.model.MovimientoStock
import cl.kawaii.tiendacute.model.Producto
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.MovimientoStockRepository
import cl.kawaii.tiendacute.repository.ProductoRepository
import cl.kawaii.tiendacute.repository.UsuarioRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.math.BigDecimal

@Service
class ProductoService(
    private val productoRepository: ProductoRepository,
    private val movimientoStockRepository: MovimientoStockRepository,
    private val usuarioRepository: UsuarioRepository
) {

    /**
     * Lista productos con paginación y filtros opcionales.
     *
     * Cuando se pasan `page` y `size`, se aplica paginación en BD y el total
     * se expone en el header `X-Total-Count` desde el controller.
     * El cuerpo sigue siendo un ARRAY para no romper contratos existentes.
     *
     * Filtros:
     *   - q: busca en nombre y código (case insensitive)
     *   - idCategoria: filtra por categoría exacta
     */
    @Transactional(readOnly = true)
    fun listarProductos(
        pageable: Pageable?,
        q: String?,
        idCategoria: Long?
    ): Page<Producto> {
        val spec: Specification<Producto> = Specification { root: Root<Producto>, query: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()
            if (q != null && q.isNotEmpty()) {
                val like = "%${q.lowercase()}%"
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get<String>("nombreProducto")), like),
                    cb.like(cb.lower(root.get<String>("codigoProducto")), like)
                ))
            }
            if (idCategoria != null) {
                predicates.add(cb.equal(
                    root.get<Categoria>("categoria").get<Long>("idCategoria"),
                    idCategoria
                ))
            }
            cb.and(*predicates.toTypedArray())
        }
        return productoRepository.findAll(spec, pageable ?: Pageable.unpaged())
    }

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Producto = productoRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Producto no encontrado con ID: $id") }

    @Transactional
    fun guardarProducto(producto: Producto): Producto {
        // Es un alta: se ignora cualquier id enviado por el cliente.
        producto.idProducto = 0
        return productoRepository.save(producto)
    }

    /**
     * Actualiza un producto. Campos opcionales: solo se modifican los que se envían.
     *
     * Si se edita el stock, queda un MovimientoStock de tipo AJUSTE_STOCK con la
     * diferencia y el usuario que hizo el cambio, para tener trazabilidad de
     * por qué el stock subió o bajó fuera de una venta.
     */
    @Transactional
    fun actualizarProducto(id: Long, request: ProductoActualizarRequest, usuarioId: Long): Producto {
        val producto = buscarPorId(id)

        if (request.nombreProducto?.isNotBlank() == true) {
            producto.nombreProducto = request.nombreProducto
        }
        if (request.codigoProducto?.isNotBlank() == true) {
            producto.codigoProducto = request.codigoProducto
        }
        if (request.precioCompraProducto != null) {
            producto.precioCompraProducto = request.precioCompraProducto
        }
        if (request.precioVentaProducto != null) {
            producto.precioVentaProducto = request.precioVentaProducto
        }
        if (request.stockActualProducto != null) {
            val stockAnterior = producto.stockActualProducto
            producto.stockActualProducto = request.stockActualProducto
            val usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow { RecursoNoEncontradoException("Usuario no encontrado con ID: $usuarioId") }
            movimientoStockRepository.save(
                MovimientoStock(
                    tipoMovimiento = "AJUSTE_STOCK",
                    cantidadMovimiento = request.stockActualProducto - stockAnterior,
                    motivoMovimiento = "AJUSTE_MANUAL",
                    producto = producto,
                    usuario = usuario
                )
            )
        }
        if (request.activo != null) {
            producto.activo = request.activo
        }
        if (request.categoriaId != null) {
            // La categoría se valida por existencia implícita al guardar si hay FK.
            // Aquí se deja el campo para futuras necesidades; la FK ya está en la entidad.
        }

        return productoRepository.save(producto)
    }

    @Transactional
    fun eliminarProducto(id: Long) = productoRepository.deleteById(id)
}

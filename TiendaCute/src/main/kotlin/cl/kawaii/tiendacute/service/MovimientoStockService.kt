package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.model.MovimientoStock
import cl.kawaii.tiendacute.repository.MovimientoStockRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.time.LocalDateTime

@Service
class MovimientoStockService(private val movimientoStockRepository: MovimientoStockRepository) {

    /**
     * Lista movimientos de stock con paginación y filtros opcionales.
     *
     * Filtros:
     *   - idProducto: filtra por producto exacto
     *   - tipo: filtra por tipo de movimiento exacto
     *   - desde, hasta: filtra por fecha de movimiento
     */
    @Transactional(readOnly = true)
    fun listarMovimientos(
        pageable: Pageable?,
        idProducto: Long?,
        tipo: String?,
        desde: LocalDateTime?,
        hasta: LocalDateTime?
    ): Page<MovimientoStock> {
        val spec: Specification<MovimientoStock> = Specification { root: Root<MovimientoStock>, query: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()
            if (idProducto != null) {
                predicates.add(cb.equal(root.get<Long>("producto_id"), idProducto))
            }
            if (tipo?.isNotBlank() == true) {
                predicates.add(cb.equal(root.get<String>("tipoMovimiento"), tipo))
            }
            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get<LocalDateTime>("creadoEn"), desde))
            }
            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get<LocalDateTime>("creadoEn"), hasta))
            }
            cb.and(*predicates.toTypedArray())
        }
        return movimientoStockRepository.findAll(spec, pageable ?: Pageable.unpaged())
    }

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): MovimientoStock = movimientoStockRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Movimiento de stock no encontrado con ID: $id") }

    @Transactional
    fun guardarMovimiento(movimientoStock: MovimientoStock): MovimientoStock {
        // Es un alta: se ignora cualquier id enviado por el cliente.
        movimientoStock.idMovimiento = 0
        return movimientoStockRepository.save(movimientoStock)
    }
}

package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.model.MovimientoStock
import cl.kawaii.tiendacute.repository.MovimientoStockRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MovimientoStockService(private val movimientoStockRepository: MovimientoStockRepository) {

    @Transactional(readOnly = true)
    fun listarMovimientos(): List<MovimientoStock> = movimientoStockRepository.findAll()

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
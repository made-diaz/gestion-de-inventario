package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.model.Pago
import cl.kawaii.tiendacute.repository.PagoRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PagoService(private val pagoRepository: PagoRepository) {

    @Transactional(readOnly = true)
    fun listarPagos(): List<Pago> = pagoRepository.findAll()

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Pago = pagoRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Pago no encontrado con ID: $id") }

    @Transactional
    fun guardarPago(pago: Pago): Pago {
        // Es un alta: se ignora cualquier id enviado por el cliente.
        pago.idPago = 0
        return pagoRepository.save(pago)
    }
}
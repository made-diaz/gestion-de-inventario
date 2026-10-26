package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.Pago
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PagoRepository : JpaRepository<Pago, Long> {
    fun findByVenta_IdVenta(ventaId: Long): List<Pago>
}
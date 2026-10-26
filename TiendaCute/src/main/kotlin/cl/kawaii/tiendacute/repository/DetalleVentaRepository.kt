package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.DetalleVenta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DetalleVentaRepository : JpaRepository<DetalleVenta, Long> {
    fun findByVenta_IdVenta(ventaId: Long): List<DetalleVenta>
}
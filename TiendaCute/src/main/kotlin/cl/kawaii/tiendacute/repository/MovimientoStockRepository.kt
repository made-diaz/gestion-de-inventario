package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.MovimientoStock
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MovimientoStockRepository : JpaRepository<MovimientoStock, Long> {
    fun findByProducto_IdProducto(idProducto: Long): List<MovimientoStock>
}

package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.MovimientoCaja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MovimientoCajaRepository : JpaRepository<MovimientoCaja, Long> {
}

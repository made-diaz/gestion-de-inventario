package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.Caja
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CajaRepository : JpaRepository<Caja, Long> {

    fun findFirstByEstadoOrderByIdCajaDesc(estado: String): Caja?

    fun existsByEstado(estado: String): Boolean
}

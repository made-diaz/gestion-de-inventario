package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.Venta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface VentaRepository : JpaRepository<Venta, Long>
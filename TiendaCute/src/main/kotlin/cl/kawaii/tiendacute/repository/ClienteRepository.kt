package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.Cliente
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional


@Repository
interface ClienteRepository : JpaRepository<Cliente, Long> {
    fun findByRutCliente(rutCliente: String): Optional<Cliente>
}
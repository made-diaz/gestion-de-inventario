package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.DocumentoTributario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface DocumentoTributarioRepository : JpaRepository<DocumentoTributario, Long> {

    fun existsByVenta_IdVenta(ventaId: Long): Boolean

    @Query(
        value = "SELECT MAX(folio) FROM documentos_tributarios WHERE tipo = ?1",
        nativeQuery = true
    )
    fun maxFolioPorTipo(tipo: String): Int?
}

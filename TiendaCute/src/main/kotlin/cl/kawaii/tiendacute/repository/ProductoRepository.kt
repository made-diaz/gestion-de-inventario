package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.Producto
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface ProductoRepository : JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {
    fun findByCodigoProducto(codigoProducto: String): Optional<Producto>

    /**
     * Lee el producto bloqueando la fila hasta el fin de la transacción.
     * Evita que dos ventas simultáneas vendan el mismo stock.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.idProducto = :id")
    fun findByIdForUpdate(@Param("id") id: Long): Optional<Producto>
}

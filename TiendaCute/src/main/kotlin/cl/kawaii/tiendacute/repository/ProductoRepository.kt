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

    /**
     * Productos con stock bajo o igual al umbral, ordenados por stock ascendente,
     * máximo 10. Devuelve Array<Any>[0]=id, [1]=nombre, [2]=código, [3]=stock.
     */
    @Query(
        value = """
            select id_producto, nombre_producto, codigo_producto, stock_actual_producto
            from productos
            where stock_actual_producto <= :umbral
            order by stock_actual_producto asc
            limit 10
        """,
        nativeQuery = true
    )
    fun obtenerProductosStockBajo(@Param("umbral") umbral: Int): List<Array<Any>>
}

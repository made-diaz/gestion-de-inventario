package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.Venta
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface VentaRepository : JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    /**
     * Carga una venta con usuario y cliente en una sola consulta (evita N+1
     * para esas relaciones). Detalles, pagos y documento se cargan por
     * consultas separadas desde el servicio, porque Venta no declara
     * propiedades @OneToMany (Kotlin+Hibernate no infiere tipos genéricos
     * de colecciones).
     */
    @Query("""
        select v from Venta v
        left join fetch v.usuario u
        left join fetch v.cliente c
        where v.idVenta = :id
    """)
    fun findVentaConDetalles(@Param("id") id: Long): Optional<Venta>

    /**
     * Listado liviano de ventas: consulta nativa con Object[] y el servicio
     * mapea a VentaListDto. Evita problemas de Hibernate con NEW, count
     * subqueries y valores null.
     */
    @Query(
        value = """
            select v.id_venta, v.fecha_venta, v.tipo_documento, v.total,
                   (select count(*) from detalle_ventas dv where dv.venta_id = v.id_venta),
                   c.nombre, u.usuario
            from ventas v
            left join clientes c on v.cliente_id = c.id_cliente
            left join usuarios u on v.usuario_id = u.id_usuario
            where (:desde is null or v.fecha_venta >= :desde)
              and (:hasta is null or v.fecha_venta <= :hasta)
              and (:idCliente is null or v.cliente_id = :idCliente)
              and (:idUsuario is null or v.usuario_id = :idUsuario)
            order by v.id_venta desc
        """,
        nativeQuery = true
    )
    fun listarVentasDto(
        @Param("desde") desde: LocalDateTime?,
        @Param("hasta") hasta: LocalDateTime?,
        @Param("idCliente") idCliente: Long?,
        @Param("idUsuario") idUsuario: Long?,
        pageable: Pageable
    ): Page<Array<Any>>
}

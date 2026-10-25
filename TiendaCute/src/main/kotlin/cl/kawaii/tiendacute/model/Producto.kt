package cl.kawaii.tiendacute.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "productos")
class Producto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    var idProducto: Long = 0,

    @Column(name = "nombre_producto", nullable = false)
    var nombreProducto: String = "",

    @Column(name = "codigo_producto", nullable = false, unique = true, length = 45)
    var codigoProducto: String = "",

    @Column(name = "precio_compra_producto", nullable = false, precision = 10, scale = 2)
    var precioCompraProducto: BigDecimal = BigDecimal.ZERO,

    @Column(name = "precio_venta_producto", nullable = false, precision = 10, scale = 2)
    var precioVentaProducto: BigDecimal = BigDecimal.ZERO,

    @Column(name = "stock_actual_producto", nullable = false)
    var stockActualProducto: Int = 0,

    @Column(nullable = false)
    var activo: Boolean = true,

    @Column(name = "creado_en", insertable = false, updatable = false)
    var creadoEn: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", nullable = false)
    var categoria: Categoria = Categoria()
)
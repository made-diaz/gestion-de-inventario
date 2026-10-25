package cl.kawaii.tiendacute.model

import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "detalle_ventas")
class DetalleVenta(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_venta")
    var idDetalleVenta: Long = 0,

    @Column(nullable = false)
    var cantidad: Int = 0,

    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    var precioUnitario: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, precision = 10, scale = 2)
    var subtotal: BigDecimal = BigDecimal.ZERO,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    var venta: Venta = Venta(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    var producto: Producto = Producto()
)
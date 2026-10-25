package cl.kawaii.tiendacute.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "movimientos_stock")
class MovimientoStock(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    var idMovimiento: Long = 0,

    @Column(name = "tipo_movimiento", nullable = false, length = 45)
    var tipoMovimiento: String = "",

    @Column(name = "cantidad_movimiento", nullable = false)
    var cantidadMovimiento: Int = 0,

    @Column(name = "motivo_movimiento", nullable = false, length = 45)
    var motivoMovimiento: String = "",

    @Column(name = "creado_en", insertable = false, updatable = false)
    var creadoEn: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    var producto: Producto = Producto(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    var usuario: Usuario = Usuario()
)
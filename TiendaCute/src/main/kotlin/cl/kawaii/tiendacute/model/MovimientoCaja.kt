package cl.kawaii.tiendacute.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "movimientos_caja")
class MovimientoCaja(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento_caja")
    var idMovimientoCaja: Long = 0,

    @Column(name = "tipo_movimiento", nullable = false, length = 20)
    var tipoMovimiento: String = "INGRESO",

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    var monto: BigDecimal = BigDecimal.ZERO,

    @Column(name = "concepto", nullable = false, length = 255)
    var concepto: String = "",

    @Column(name = "referencia_tipo", length = 20)
    var referenciaTipo: String? = null,

    @Column(name = "referencia_id")
    var referenciaId: Long? = null,

    @Column(name = "fecha_movimiento", insertable = false, updatable = false)
    var fechaMovimiento: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caja_id", nullable = false)
    var caja: Caja = Caja(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    var usuario: Usuario = Usuario()
)

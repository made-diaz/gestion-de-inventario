package cl.kawaii.tiendacute.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "ventas")
class Venta(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    var idVenta: Long = 0,

    @Column(name = "fecha_venta", insertable = false, updatable = false)
    var fechaVenta: LocalDateTime? = null,

    @Column(name = "total_neto", nullable = false, precision = 10, scale = 2)
    var totalNeto: BigDecimal = BigDecimal.ZERO,

    @Column(name = "iva", nullable = false, precision = 10, scale = 2)
    var iva: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    var total: BigDecimal = BigDecimal.ZERO,

    @Column(name = "tipo_documento", nullable = false, length = 20)
    var tipoDocumento: String = "",

    @Column(name = "estado_venta", nullable = false, length = 20)
    var estadoVenta: String = "",

    @Column(name = "creado_en", insertable = false, updatable = false)
    var creadoEn: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    var usuario: Usuario = Usuario(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = true)
    var cliente: Cliente? = null
)
package cl.kawaii.tiendacute.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "pagos")
class Pago(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    var idPago: Long = 0,

    @Column(name = "metodo_pago", nullable = false, length = 50)
    var metodoPago: String = "",

    @Column(name = "monto_pago", nullable = false, precision = 10, scale = 2)
    var montoPago: BigDecimal = BigDecimal.ZERO,

    @Column(length = 255)
    var referencia: String? = null,

    @Column(name = "creado_en", insertable = false, updatable = false)
    var creadoEn: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    var venta: Venta = Venta()
)
package cl.kawaii.tiendacute.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "cajas")
class Caja(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caja")
    var idCaja: Long = 0,

    @Column(name = "fecha_apertura", insertable = false, updatable = false)
    var fechaApertura: LocalDateTime? = null,

    @Column(name = "fecha_cierre")
    var fechaCierre: LocalDateTime? = null,

    @Column(name = "monto_inicial", nullable = false, precision = 10, scale = 2)
    var montoInicial: BigDecimal = BigDecimal.ZERO,

    @Column(name = "monto_final", precision = 10, scale = 2)
    var montoFinal: BigDecimal? = null,

    @Column(name = "estado_caja", nullable = false, length = 20)
    var estado: String = "ABIERTA",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    var usuario: Usuario = Usuario()
)

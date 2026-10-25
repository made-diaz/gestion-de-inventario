package cl.kawaii.tiendacute.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "documentos_tributarios")
class DocumentoTributario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_tributario")
    var idDocumentoTributario: Long = 0,

    @Column(nullable = false, length = 20)
    var tipo: String = "",

    @Column(nullable = false)
    var folio: Int = 0,

    @Column(name = "estado_sii", nullable = false, length = 20)
    var estadoSii: String = "",

    @Column(columnDefinition = "TEXT")
    var xml: String? = null,

    @Column(name = "track_id", length = 100)
    var trackId: String? = null,

    @Column(name = "fecha_envio")
    var fechaEnvio: LocalDateTime? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venta_id", nullable = false)
    var venta: Venta = Venta()
)
package cl.kawaii.tiendacute.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "clientes")
class Cliente(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cliente")
    var idCliente: Long = 0,

    @Column(name = "rut_cliente", length = 12)
    var rutCliente: String? = null,

    @Column(name = "nombre")
    var nombre: String? = null,

    @Column(name = "razon_social_cliente")
    var razonSocialCliente: String? = null,

    @Column(name = "giro_cliente")
    var giroCliente: String? = null,

    @Column(name = "direccion_cliente")
    var direccionCliente: String? = null,

    @Column(name = "comuna_cliente", length = 100)
    var comunaCliente: String? = null,

    @Column(name = "ciudad_cliente", length = 100)
    var ciudadCliente: String? = null,

    @Column(name = "email_cliente")
    var emailCliente: String? = null,

    @Column(name = "telefono_cliente", length = 50)
    var telefonoCliente: String? = null,

    @Column(name = "creado_en", insertable = false, updatable = false)
    var creadoEn: LocalDateTime? = null
)
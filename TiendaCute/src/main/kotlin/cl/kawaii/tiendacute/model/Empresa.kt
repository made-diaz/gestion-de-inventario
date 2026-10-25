package cl.kawaii.tiendacute.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "empresas")
class Empresa(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_empresa")
    var idEmpresa: Long = 0,

    @Column(name = "rut_empresa", nullable = false, unique = true, length = 12)
    var rutEmpresa: String = "",

    @Column(name = "e_razon_social")
    var eRazonSocial: String = "",

    @Column(name = "giro_empresa")
    var giroEmpresa: String? = null,

    @Column(name = "direccion_empresa")
    var direccionEmpresa: String? = null,

    @Column(name = "comuna_empresa", length = 100)
    var comunaEmpresa: String? = null,

    @Column(name = "ciudad_empresa", length = 100)
    var ciudadEmpresa: String? = null,

    @Column(name = "creado_en", insertable = false, updatable = false)
    var creadoEn : LocalDateTime? = null
)
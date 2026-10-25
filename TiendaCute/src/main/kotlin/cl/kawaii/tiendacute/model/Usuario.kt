package cl.kawaii.tiendacute.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "usuarios")
class Usuario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    var idUsuario: Long = 0,

    @Column(name = "usuario", nullable = false, unique = true, length = 50)
    var usuario: String = "",

    // Se acepta al crear/actualizar, pero nunca se serializa en las respuestas.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Column(name = "password", nullable = false)
    var password: String = "",

    @Column(name = "rol_usuario", nullable = false, length = 20)
    var rolUsuario: String = "",

    @Column(name = "activo_usuario", nullable = false)
    var activoUsuario: Boolean = true,

    @Column(name = "creado_en", insertable = false, updatable = false)
    var creadoEn: LocalDateTime? = null
)
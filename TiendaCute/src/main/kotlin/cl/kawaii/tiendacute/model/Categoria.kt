package cl.kawaii.tiendacute.model

import jakarta.persistence.*

@Entity
@Table(name = "categorias")
class Categoria(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    var idCategoria: Long = 0,

    @Column(name = "nombre_categoria", nullable = false, length = 100)
    var nombreCategoria: String = ""
)
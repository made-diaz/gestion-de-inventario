package cl.kawaii.tiendacute.repository

import cl.kawaii.tiendacute.model.Usuario
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UsuarioRepository : JpaRepository <Usuario, Long>{
    fun findByUsuario(usuario: String): Optional<Usuario>
}
package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UsuarioService(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional(readOnly = true)
    fun listarUsuarios(): List<Usuario> = usuarioRepository.findAll()

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Usuario = usuarioRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Usuario no encontrado con ID: $id") }

    @Transactional
    fun guardarUsuario(usuario: Usuario): Usuario {
        validarPassword(usuario.usuario, usuario.password)
        // Es un alta: se ignora cualquier id enviado por el cliente.
        usuario.idUsuario = 0
        usuario.password = passwordEncoder.encode(usuario.password)
        return usuarioRepository.save(usuario)
    }

    /**
     * Borra un usuario, salvo que sea el último administrador activo: si se
     * fuera, nadie podría volver a administrar el sistema.
     */
    @Transactional
    fun eliminarUsuario(id: Long) {
        val usuario = buscarPorId(id)

        val esUltimoAdminActivo = usuario.rolUsuario.equals(ROL_ADMIN, ignoreCase = true) &&
            usuario.activoUsuario &&
            usuarioRepository.countByRolUsuarioAndActivoUsuario(ROL_ADMIN, true) <= 1

        if (esUltimoAdminActivo) {
            throw ReglaNegocioException(
                "No se puede eliminar el último administrador activo: el sistema quedaría sin acceso"
            )
        }

        usuarioRepository.delete(usuario)
    }

    companion object {
        const val MINIMO_CARACTERES_PASSWORD = 8
        private const val ROL_ADMIN = "ADMIN"

        /**
         * Reglas mínimas de contraseña, aplicadas en el servidor: el cliente
         * puede mentir, el backend no.
         */
        fun validarPassword(nombreUsuario: String, password: String) {
            if (password.length < MINIMO_CARACTERES_PASSWORD) {
                throw ReglaNegocioException(
                    "La contraseña debe tener al menos $MINIMO_CARACTERES_PASSWORD caracteres"
                )
            }
            if (password.equals(nombreUsuario, ignoreCase = true)) {
                throw ReglaNegocioException("La contraseña no puede ser igual al nombre de usuario")
            }
            if (!password.any { it.isLetter() } || !password.any { it.isDigit() }) {
                throw ReglaNegocioException("La contraseña debe combinar letras y números")
            }
        }
    }
}

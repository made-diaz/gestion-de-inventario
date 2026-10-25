package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.CambiarContrasenaRequest
import cl.kawaii.tiendacute.dto.UsuarioActualizarRequest
import cl.kawaii.tiendacute.exception.OperacionNoPermitidaException
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Service
class UsuarioService(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder,
    gestorTransacciones: PlatformTransactionManager
) {

    /**
     * El chequeo y el commit del borrado ocurren dentro de este lock. Si la
     * transacción cerrara después de soltarlo, dos borrados simultáneos podrían
     * pasar los dos el chequeo y dejar el sistema sin administrador.
     *
     * Es un lock de JVM: alcanza para una instancia, igual que el contador de
     * intentos. Con varias instancias esto se resuelve en la base.
     */
    private val bloqueoEliminacion = ReentrantLock()
    private val transaccion = TransactionTemplate(gestorTransacciones)

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
     *
     * Borrar un id inexistente sigue siendo un no-op (204), como antes.
     */
    fun eliminarUsuario(id: Long) {
        bloqueoEliminacion.withLock {
            transaccion.executeWithoutResult {
                val usuario = usuarioRepository.findById(id).orElse(null)
                    ?: return@executeWithoutResult

                val esUltimoAdminActivo =
                    usuario.rolUsuario.equals(ROL_ADMIN, ignoreCase = true) &&
                        usuario.activoUsuario &&
                        usuarioRepository.countByRolUsuarioAndActivoUsuario(ROL_ADMIN, true) <= 1

                if (esUltimoAdminActivo) {
                    throw OperacionNoPermitidaException(
                        "No se puede eliminar el último administrador activo: el sistema quedaría sin acceso"
                    )
                }

                usuarioRepository.delete(usuario)
            }
        }
    }

    /**
     * Actualiza nombre, rol y estado de un usuario. Solo ADMIN puede editar
     * cualquier usuario; no se permite deshabilitar ni quitar el rol ADMIN al
     * último administrador activo (409).
     */
    @Transactional
    fun actualizarUsuario(id: Long, request: UsuarioActualizarRequest, usuarioIdQueHaceCambio: Long): Usuario {
        val usuario = buscarPorId(id)

        // Captura si era admin activo ANTES de tocar nada.
        val eraAdminActivoAntes =
            usuario.rolUsuario.equals(ROL_ADMIN, ignoreCase = true) && usuario.activoUsuario

        // Si este usuario va a dejar de ser admin activo, hay que asegurar que quede al menos otro.
        // El count se hace ANTES de modificar el objeto para evitar que Hibernate haga flush
        // del cambio pendiente y falsifique el conteo.
        if (eraAdminActivoAntes) {
            val dejaraDeSerAdminActivo =
                (request.activoUsuario != null && !request.activoUsuario) ||
                    (request.rolUsuario?.isNotBlank() == true &&
                        !request.rolUsuario.equals(ROL_ADMIN, ignoreCase = true))

            if (dejaraDeSerAdminActivo) {
                val adminActivos = usuarioRepository.countByRolUsuarioAndActivoUsuario(ROL_ADMIN, true)
                if (adminActivos <= 1L) {
                    throw OperacionNoPermitidaException(
                        "No se puede deshabilitar ni quitar el rol ADMIN al último administrador activo: el sistema quedaría sin acceso"
                    )
                }
            }
        }

        // Aplica los cambios.
        if (request.usuario?.isNotBlank() == true) {
            usuario.usuario = request.usuario
        }
        if (request.rolUsuario?.isNotBlank() == true) {
            usuario.rolUsuario = request.rolUsuario
        }
        if (request.activoUsuario != null) {
            usuario.activoUsuario = request.activoUsuario
        }

        return usuarioRepository.save(usuario)
    }

    /**
     * Cambia la contraseña de un usuario.
     * Puede hacerlo el propio usuario (usuarioIdQueHaceCambio == id) o un ADMIN.
     * Valida la política de contraseñas.
     */
    @Transactional
    fun actualizarContraseña(id: Long, request: CambiarContrasenaRequest, usuarioIdQueHaceCambio: Long): Usuario {
        val solicitante = usuarioRepository.findById(usuarioIdQueHaceCambio)
            .orElseThrow { RecursoNoEncontradoException("Usuario no encontrado con ID: $usuarioIdQueHaceCambio") }

        val esPropioUsuario = id == usuarioIdQueHaceCambio
        val esAdmin = solicitante.rolUsuario.equals(ROL_ADMIN, ignoreCase = true)

        if (!esPropioUsuario && !esAdmin) {
            throw OperacionNoPermitidaException("Solo puedes cambiar tu propia contraseña, o ser ADMIN para cambiar la de otro usuario")
        }

        val usuario = buscarPorId(id)
        validarPassword(usuario.usuario, request.password)
        usuario.password = passwordEncoder.encode(request.password)
        return usuarioRepository.save(usuario)
    }

    companion object {
        const val MINIMO_CARACTERES_PASSWORD = 8
        private const val ROL_ADMIN = "ADMIN"

        /** Reglas mínimas de contraseña, aplicadas en el servidor: el cliente puede mentir, el backend no. */
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

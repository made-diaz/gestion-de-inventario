package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.UsuarioRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service

@Service
class UsuarioService(
    private val usuarioRepository: UsuarioRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional(readOnly = true)
    fun listarUsuarios(): List<Usuario> = usuarioRepository.findAll()

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Usuario = usuarioRepository.findById(id)
        .orElseThrow{ RecursoNoEncontradoException("Usuario no encontrado con ID: $id") }

    @Transactional
    fun guardarUsuario(usuario: Usuario): Usuario {
        // Es un alta: se ignora cualquier id enviado por el cliente.
        usuario.idUsuario = 0
        usuario.password = passwordEncoder.encode(usuario.password)
        return usuarioRepository.save(usuario)
    }

    @Transactional
    fun eliminarUsuario(id: Long) = usuarioRepository.deleteById(id)
}
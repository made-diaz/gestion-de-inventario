package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.CambiarContrasenaRequest
import cl.kawaii.tiendacute.dto.UsuarioActualizarRequest
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.service.UsuarioService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/usuarios")
class UsuarioController(private val usuarioService: UsuarioService) {

    @GetMapping
    fun listarUsuarios(): List<Usuario> = usuarioService.listarUsuarios()

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<Usuario> =
        ResponseEntity.ok(usuarioService.buscarPorId(id))

    @PostMapping
    fun crearUsuario(@RequestBody usuario: Usuario): ResponseEntity<Usuario> =
        ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.guardarUsuario(usuario))

    @PutMapping("/{id}")
    fun actualizarUsuario(
        @PathVariable id: Long,
        @RequestBody request: UsuarioActualizarRequest,
        @RequestParam usuarioId: Long
    ): ResponseEntity<Usuario> =
        ResponseEntity.ok(usuarioService.actualizarUsuario(id, request, usuarioId))

    @PutMapping("/{id}/contrasena")
    fun actualizarContrasena(
        @PathVariable id: Long,
        @RequestBody request: CambiarContrasenaRequest,
        @RequestParam usuarioId: Long
    ): ResponseEntity<Void> {
        usuarioService.actualizarContraseña(id, request, usuarioId)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping("/{id}")
    fun eliminarUsuario(@PathVariable id: Long): ResponseEntity<Void> {
        usuarioService.eliminarUsuario(id)
        return ResponseEntity.noContent().build()
    }
}

package cl.kawaii.tiendacute.controller

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

    @DeleteMapping("/{id}")
    fun eliminarUsuario(@PathVariable id: Long): ResponseEntity<Void> {
        usuarioService.eliminarUsuario(id)
        return ResponseEntity.noContent().build()
    }
}
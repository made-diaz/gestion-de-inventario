package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.ClienteActualizarRequest
import cl.kawaii.tiendacute.model.Cliente
import cl.kawaii.tiendacute.service.ClienteService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/clientes")
class ClienteController(private val clienteService: ClienteService) {

    @GetMapping
    fun listarClientes(): List<Cliente> = clienteService.listarClientes()

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<Cliente> =
        ResponseEntity.ok(clienteService.buscarPorId(id))

    @PostMapping
    fun crearCliente(@RequestBody cliente: Cliente): ResponseEntity<Cliente> =
        ResponseEntity.status(HttpStatus.CREATED).body(clienteService.guardarCliente(cliente))

    @PutMapping("/{id}")
    fun actualizarCliente(
        @PathVariable id: Long,
        @RequestBody request: ClienteActualizarRequest
    ): ResponseEntity<Cliente> =
        ResponseEntity.ok(clienteService.actualizarCliente(id, request))

    @DeleteMapping("/{id}")
    fun eliminarCliente(@PathVariable id: Long): ResponseEntity<Void> {
        clienteService.eliminarCliente(id)
        return ResponseEntity.noContent().build()
    }
}

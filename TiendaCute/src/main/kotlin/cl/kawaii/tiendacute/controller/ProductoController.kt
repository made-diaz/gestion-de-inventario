package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.ProductoActualizarRequest
import cl.kawaii.tiendacute.model.Producto
import cl.kawaii.tiendacute.service.ProductoService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/productos")
class ProductoController(private val productoService: ProductoService) {

    @GetMapping
    fun listarProductos(): List<Producto> = productoService.listarProductos()

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<Producto> =
        ResponseEntity.ok(productoService.buscarPorId(id))

    @PostMapping
    fun crearProducto(@RequestBody producto: Producto): ResponseEntity<Producto> =
        ResponseEntity.status(HttpStatus.CREATED).body(productoService.guardarProducto(producto))

    @PutMapping("/{id}")
    fun actualizarProducto(
        @PathVariable id: Long,
        @RequestBody request: ProductoActualizarRequest,
        @RequestParam usuarioId: Long
    ): ResponseEntity<Producto> =
        ResponseEntity.ok(productoService.actualizarProducto(id, request, usuarioId))

    @DeleteMapping("/{id}")
    fun eliminarProducto(@PathVariable id: Long): ResponseEntity<Void> {
        productoService.eliminarProducto(id)
        return ResponseEntity.noContent().build()
    }
}

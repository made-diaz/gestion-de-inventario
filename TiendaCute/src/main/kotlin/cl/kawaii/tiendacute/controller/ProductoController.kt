package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.model.Producto
import cl.kawaii.tiendacute.service.ProductoService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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

    @DeleteMapping("/{id}")
    fun eliminarProducto(@PathVariable id: Long): ResponseEntity<Void> {
        productoService.eliminarProducto(id)
        return ResponseEntity.noContent().build()
    }
}
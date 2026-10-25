package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.ProductoActualizarRequest
import cl.kawaii.tiendacute.model.Producto
import cl.kawaii.tiendacute.service.ProductoService
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/productos")
class ProductoController(private val productoService: ProductoService) {

    @GetMapping
    fun listarProductos(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) idCategoria: Long?,
        response: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<List<Producto>> {
        val pageable = if (page != null && size != null) PageRequest.of(page, size) else null
        val pagina = productoService.listarProductos(pageable, q, idCategoria)
        if (pageable != null) {
            response.setHeader("X-Total-Count", pagina.totalElements.toString())
        }
        return ResponseEntity.ok(pagina.content)
    }

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

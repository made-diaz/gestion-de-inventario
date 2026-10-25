package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.CategoriaActualizarRequest
import cl.kawaii.tiendacute.model.Categoria
import cl.kawaii.tiendacute.service.CategoriaService
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categorias")
class CategoriaController(private val categoriaService: CategoriaService) {

    @GetMapping
    fun listarCategorias(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        response: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<List<Categoria>> {
        val pageable = if (page != null && size != null) PageRequest.of(page, size) else null
        val pagina = categoriaService.listarCategorias(pageable)
        if (pageable != null) {
            response.setHeader("X-Total-Count", pagina.totalElements.toString())
        }
        return ResponseEntity.ok(pagina.content)
    }

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<Categoria> =
        ResponseEntity.ok(categoriaService.buscarPorId(id))

    @PostMapping
    fun crearCategoria(@RequestBody categoria: Categoria): ResponseEntity<Categoria> =
        ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.guardarCategoria(categoria))

    @PutMapping("/{id}")
    fun actualizarCategoria(
        @PathVariable id: Long,
        @RequestBody request: CategoriaActualizarRequest
    ): ResponseEntity<Categoria> =
        ResponseEntity.ok(categoriaService.actualizarCategoria(id, request))

    @DeleteMapping("/{id}")
    fun eliminarCategoria(@PathVariable id: Long): ResponseEntity<Void> {
        categoriaService.eliminarCategoria(id)
        return ResponseEntity.noContent().build()
    }
}

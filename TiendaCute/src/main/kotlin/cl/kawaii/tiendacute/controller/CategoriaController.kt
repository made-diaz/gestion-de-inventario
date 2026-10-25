package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.model.Categoria
import cl.kawaii.tiendacute.service.CategoriaService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/categorias")
class CategoriaController(private val categoriaService: CategoriaService) {

    @GetMapping
    fun listarCategorias(): List<Categoria> = categoriaService.listarCategorias()

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<Categoria> =
        ResponseEntity.ok(categoriaService.buscarPorId(id))

    @PostMapping
    fun crearCategoria(@RequestBody categoria: Categoria): ResponseEntity<Categoria> =
        ResponseEntity.status(HttpStatus.CREATED).body(categoriaService.guardarCategoria(categoria))

    @DeleteMapping("/{id}")
    fun eliminarCategoria(@PathVariable id: Long): ResponseEntity<Void> {
        categoriaService.eliminarCategoria(id)
        return ResponseEntity.noContent().build()
    }
}
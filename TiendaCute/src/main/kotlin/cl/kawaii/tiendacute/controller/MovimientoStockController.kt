package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.model.MovimientoStock
import cl.kawaii.tiendacute.service.MovimientoStockService
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/movimientos-stock")
class MovimientoStockController(private val movimientoStockService: MovimientoStockService) {

    @GetMapping
    fun listarMovimientos(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) idProducto: Long?,
        @RequestParam(required = false) tipo: String?,
        @RequestParam(required = false) desde: LocalDateTime?,
        @RequestParam(required = false) hasta: LocalDateTime?,
        response: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<List<MovimientoStock>> {
        val pageable = if (page != null && size != null) PageRequest.of(page, size) else null
        val pagina = movimientoStockService.listarMovimientos(pageable, idProducto, tipo, desde, hasta)
        if (pageable != null) {
            response.setHeader("X-Total-Count", pagina.totalElements.toString())
        }
        return ResponseEntity.ok(pagina.content)
    }

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<MovimientoStock> =
        ResponseEntity.ok(movimientoStockService.buscarPorId(id))

    @PostMapping
    fun registrarMovimiento(@RequestBody movimientoStock: MovimientoStock): ResponseEntity<MovimientoStock> =
        ResponseEntity.status(HttpStatus.CREATED).body(movimientoStockService.guardarMovimiento(movimientoStock))
}

package cl.kawaii.tiendacute.controller


import cl.kawaii.tiendacute.model.MovimientoStock
import cl.kawaii.tiendacute.service.MovimientoStockService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/movimientos-stock")
class MovimientoStockController(private val movimientoStockService: MovimientoStockService) {

    @GetMapping
    fun listarMovimientos(): List<MovimientoStock> = movimientoStockService.listarMovimientos()

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<MovimientoStock> =
        ResponseEntity.ok(movimientoStockService.buscarPorId(id))

    @PostMapping
    fun registrarMovimiento(@RequestBody movimientoStock: MovimientoStock): ResponseEntity<MovimientoStock> =
        ResponseEntity.status(HttpStatus.CREATED).body(movimientoStockService.guardarMovimiento(movimientoStock))
}
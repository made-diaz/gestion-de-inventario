package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.VentaRequest
import cl.kawaii.tiendacute.model.Venta
import cl.kawaii.tiendacute.service.VentaService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/ventas")
class VentaController(private val ventaService: VentaService) {

    @PostMapping
    fun registrarVenta(@RequestBody request: VentaRequest): ResponseEntity<Venta> {
        val ventaRegistrada = ventaService.registrarVenta(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaRegistrada)
    }
}
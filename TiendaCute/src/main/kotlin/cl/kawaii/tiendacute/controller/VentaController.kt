package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.VentaRequest
import cl.kawaii.tiendacute.model.Venta
import cl.kawaii.tiendacute.service.VentaService
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/ventas")
class VentaController(private val ventaService: VentaService) {

    @GetMapping
    fun listarVentas(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) desde: LocalDateTime?,
        @RequestParam(required = false) hasta: LocalDateTime?,
        @RequestParam(required = false) idCliente: Long?,
        @RequestParam(required = false) idUsuario: Long?,
        response: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<List<Venta>> {
        val pageable = if (page != null && size != null) PageRequest.of(page, size) else null
        val pagina = ventaService.listarVentas(pageable, desde, hasta, idCliente, idUsuario)
        if (pageable != null) {
            response.setHeader("X-Total-Count", pagina.totalElements.toString())
        }
        return ResponseEntity.ok(pagina.content)
    }

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<Venta> =
        ResponseEntity.ok(ventaService.buscarPorId(id))

    @PostMapping
    fun registrarVenta(@RequestBody request: VentaRequest): ResponseEntity<Venta> {
        val ventaRegistrada = ventaService.registrarVenta(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaRegistrada)
    }
}

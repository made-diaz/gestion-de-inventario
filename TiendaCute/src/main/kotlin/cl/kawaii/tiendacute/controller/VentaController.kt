package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.VentaDetalleDto
import cl.kawaii.tiendacute.dto.VentaListDto
import cl.kawaii.tiendacute.dto.VentaRequest
import cl.kawaii.tiendacute.service.VentaService
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/ventas")
class VentaController(private val ventaService: VentaService) {

    /**
     * Listado liviano de ventas con paginación y filtros.
     * El total se expone en el header X-Total-Count.
     */
    @GetMapping
    fun listarVentas(
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?,
        @RequestParam(required = false) desde: LocalDateTime?,
        @RequestParam(required = false) hasta: LocalDateTime?,
        @RequestParam(required = false) idCliente: Long?,
        @RequestParam(required = false) idUsuario: Long?,
        response: jakarta.servlet.http.HttpServletResponse
    ): ResponseEntity<List<VentaListDto>> {
        val pageable = if (page != null && size != null) PageRequest.of(page, size) else null
        val pagina = ventaService.listarVentas(pageable, desde, hasta, idCliente, idUsuario)
        if (pageable != null) {
            response.setHeader("X-Total-Count", pagina.totalElements.toString())
        }
        return ResponseEntity.ok(pagina.content)
    }

    /**
     * Detalle completo de una venta: líneas, pagos, cajero, cliente y documento
     * tributario en una sola consulta.
     */
    @GetMapping("/{id}")
    fun buscarDetalle(@PathVariable id: Long): ResponseEntity<VentaDetalleDto> =
        ResponseEntity.ok(ventaService.buscarDetalle(id))

    /**
     * Registra una venta. Acepta pagos opcionales [{metodoPago, montoPago}]
     * registrados en la misma transacción y validados contra el total del
     * servidor.
     */
    @PostMapping
    fun registrarVenta(@RequestBody request: VentaRequest): ResponseEntity<VentaDetalleDto> {
        val ventaRegistrada = ventaService.registrarVenta(request)
        val detalle = ventaService.buscarDetalle(ventaRegistrada.idVenta)
        return ResponseEntity.status(HttpStatus.CREATED).body(detalle)
    }
}

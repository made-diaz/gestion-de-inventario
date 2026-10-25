package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.PagoRequest
import cl.kawaii.tiendacute.model.Pago
import cl.kawaii.tiendacute.service.PagoService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/pagos")
class PagoController(private val pagoService: PagoService) {

    @GetMapping
    fun listarPagos(): List<Pago> = pagoService.listarPagos()

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<Pago> =
        ResponseEntity.ok(pagoService.buscarPorId(id))

    /**
     * Registra un pago contra una venta. El cuerpo es un PagoRequest, no el
     * entity Pago: el cliente no debe poder setear el id, la venta ni el monto
     * pagado acumulado de la venta.
     */
    @PostMapping
    fun registrarPago(@RequestBody request: PagoRequest): ResponseEntity<Pago> =
        ResponseEntity.status(HttpStatus.CREATED).body(pagoService.guardarPago(request))
}

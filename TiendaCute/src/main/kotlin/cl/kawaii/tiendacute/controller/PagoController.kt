package cl.kawaii.tiendacute.controller

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

    @PostMapping
    fun registrarPago(@RequestBody pago: Pago): ResponseEntity<Pago> =
        ResponseEntity.status(HttpStatus.CREATED).body(pagoService.guardarPago(pago))
}
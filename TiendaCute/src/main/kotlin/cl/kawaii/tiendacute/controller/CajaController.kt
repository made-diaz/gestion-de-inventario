package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.AbrirCajaRequest
import cl.kawaii.tiendacute.dto.CerrarCajaRequest
import cl.kawaii.tiendacute.model.Caja
import cl.kawaii.tiendacute.model.MovimientoCaja
import cl.kawaii.tiendacute.service.CajaService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/cajas")
class CajaController(private val cajaService: CajaService) {

    @GetMapping
    fun listarCajas(): List<Caja> = cajaService.listarCajas()

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<Caja> =
        ResponseEntity.ok(cajaService.buscarPorId(id))

    @GetMapping("/estado")
    fun estado(): ResponseEntity<Caja?> =
        ResponseEntity.ok(cajaService.estadoCaja())

    @GetMapping("/movimientos")
    fun listarMovimientos(): List<MovimientoCaja> = cajaService.listarMovimientos()

    @PostMapping("/abrir")
    fun abrirCaja(@RequestBody request: AbrirCajaRequest): ResponseEntity<Caja> =
        ResponseEntity.status(HttpStatus.CREATED).body(cajaService.abrirCaja(request.usuarioId, request.montoInicial))

    @PostMapping("/cerrar")
    fun cerrarCaja(@RequestBody request: CerrarCajaRequest): ResponseEntity<Caja> =
        ResponseEntity.ok(cajaService.cerrarCaja(request.usuarioId, request.montoFinal))}

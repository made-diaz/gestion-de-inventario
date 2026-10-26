package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.ReporteResumenDto
import cl.kawaii.tiendacute.service.ReporteService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reportes")
class ReporteController(private val reporteService: ReporteService) {

    /**
     * Resumen del día (zona America/Santiago) y productos con stock bajo.
     *
     * Parámetros:
     *   - umbralStock: stock máximo para considerar un producto como "bajo" (default 5).
     *
     * Todas las agregaciones se hacen en base de datos mediante consultas nativas.
     */
    @GetMapping("/resumen")
    fun obtenerResumen(
        @RequestParam(defaultValue = "5") umbralStock: Int
    ): ResponseEntity<ReporteResumenDto> =
        ResponseEntity.ok(reporteService.obtenerResumen(umbralStock))
}

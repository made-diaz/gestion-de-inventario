package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.model.DocumentoTributario
import cl.kawaii.tiendacute.dto.EmitirDocumentoRequest
import cl.kawaii.tiendacute.service.DocumentoTributarioService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/documentos-tributarios")
class DocumentoTributarioController(private val documentoService: DocumentoTributarioService) {

    @GetMapping
    fun listarDocumentos(): List<DocumentoTributario> = documentoService.listarDocumentos()

    @GetMapping("/{id}")
    fun buscarPorId(@PathVariable id: Long): ResponseEntity<DocumentoTributario> =
        ResponseEntity.ok(documentoService.buscarPorId(id))

    /**
     * Crea un documento directamente (mantiene la ruta original para
     * compatibilidad). La emisión con validaciones se hace por /emitir.
     */
    @PostMapping
    fun crearDocumento(@RequestBody documento: DocumentoTributario): ResponseEntity<DocumentoTributario> =
        ResponseEntity.status(HttpStatus.CREATED).body(documentoService.guardarDocumento(documento))

    /**
     * Emite un documento tributario sobre una venta pagada completa.
     * Valida saldo pendiente, folio correlativo y que no exista ya uno para
     * esa venta.
     */
    @PostMapping("/emitir")
    fun emitirDocumento(@RequestBody request: EmitirDocumentoRequest): ResponseEntity<DocumentoTributario> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(documentoService.emitirDocumento(request.ventaId, request.tipo))
}

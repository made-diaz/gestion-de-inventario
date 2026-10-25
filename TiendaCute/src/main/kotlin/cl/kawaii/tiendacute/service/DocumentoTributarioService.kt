package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.exception.OperacionNoPermitidaException
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.DocumentoTributario
import cl.kawaii.tiendacute.model.Venta
import cl.kawaii.tiendacute.repository.DocumentoTributarioRepository
import cl.kawaii.tiendacute.repository.VentaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class DocumentoTributarioService(
    private val documentoRepository: DocumentoTributarioRepository,
    private val ventaRepository: VentaRepository
) {

    private companion object {
        const val ESTADO_EMITIDO = "EMITIDO"
    }

    @Transactional(readOnly = true)
    fun listarDocumentos(): List<DocumentoTributario> = documentoRepository.findAll()

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): DocumentoTributario = documentoRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Documento tributario no encontrado con ID: $id") }

    @Transactional
    fun guardarDocumento(documento: DocumentoTributario): DocumentoTributario {
        // Es un alta: se ignora cualquier id enviado por el cliente.
        documento.idDocumentoTributario = 0
        return documentoRepository.save(documento)
    }

    /**
     * Emite un documento tributario (boleta o factura) sobre una venta ya
     * pagada completa.
     *
     * Reglas:
     *  - La venta debe existir.
     *  - El saldo pendiente de la venta debe ser 0 (pagada completa).
     *  - No puede existir ya un documento para esa venta.
     *  - El folio es correlativo automático por tipo de documento.
     *  - El estado SII parte en EMITIDO y se genera un XML simulado.
     */
    @Transactional
    fun emitirDocumento(ventaId: Long, tipo: String): DocumentoTributario {
        val venta = ventaRepository.findById(ventaId)
            .orElseThrow {
                RecursoNoEncontradoException("Venta no encontrada con ID: $ventaId")
            }

        if (venta.saldoPendiente().compareTo(BigDecimal.ZERO) != 0) {
            throw ReglaNegocioException(
                "No se puede emitir documento: la venta #$ventaId no está pagada completa. " +
                    "Saldo pendiente: ${venta.saldoPendiente()}"
            )
        }

        if (documentoRepository.existsByVenta_IdVenta(ventaId)) {
            throw OperacionNoPermitidaException(
                "La venta #$ventaId ya tiene un documento tributario emitido"
            )
        }

        val folio = (documentoRepository.maxFolioPorTipo(tipo) ?: 0) + 1
        val fechaEmision = LocalDateTime.now()

        return documentoRepository.save(
            DocumentoTributario(
                tipo = tipo,
                folio = folio,
                estadoSii = ESTADO_EMITIDO,
                fechaEnvio = fechaEmision,
                xml = generarXml(venta, tipo, folio, fechaEmision),
                venta = venta
            )
        )
    }

    private fun generarXml(venta: Venta, tipo: String, folio: Int, fecha: LocalDateTime): String {
        val clienteRut = venta.cliente?.rutCliente ?: "SIN RUT"
        val clienteNombre = venta.cliente?.nombre ?: "Consumidor final"
        return """
            |<?xml version="1.0" encoding="UTF-8"?>
            |<DocumentoTributario>
            |  <Tipo>${tipo}</Tipo>
            |  <Folio>$folio</Folio>
            |  <FechaEmision>${fecha.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}</FechaEmision>
            |  <EstadoSii>${ESTADO_EMITIDO}</EstadoSii>
            |  <Venta id="${venta.idVenta}" total="${venta.total}" />
            |  <Cliente rut="$clienteRut" nombre="$clienteNombre" />
            |  <Total>${venta.total.setScale(2, java.math.RoundingMode.HALF_UP)}</Total>
            |</DocumentoTributario>
        """.trimMargin()
    }
}

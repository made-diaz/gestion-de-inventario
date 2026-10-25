package cl.kawaii.tiendacute.service


import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.model.DocumentoTributario
import cl.kawaii.tiendacute.repository.DocumentoTributarioRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DocumentoTributarioService(private val documentoRepository: DocumentoTributarioRepository) {

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
}
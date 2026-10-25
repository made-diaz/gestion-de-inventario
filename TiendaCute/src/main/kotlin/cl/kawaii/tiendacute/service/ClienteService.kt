package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.ClienteActualizarRequest
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.Cliente
import cl.kawaii.tiendacute.repository.ClienteRepository
import cl.kawaii.tiendacute.util.RutUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClienteService(private val clienteRepository: ClienteRepository) {

    @Transactional(readOnly = true)
    fun listarClientes(): List<Cliente> = clienteRepository.findAll()

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Cliente = clienteRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Cliente no encontrado con ID: $id") }

    @Transactional
    fun guardarCliente(cliente: Cliente): Cliente {
        // Es un alta: se ignora cualquier id enviado por el cliente.
        cliente.idCliente = 0
        return clienteRepository.save(cliente)
    }

    @Transactional
    fun actualizarCliente(id: Long, request: ClienteActualizarRequest): Cliente {
        val cliente = buscarPorId(id)

        if (request.rutCliente?.isNotBlank() == true) {
            if (!RutUtil.validar(request.rutCliente)) {
                throw ReglaNegocioException("El RUT ${request.rutCliente} no es válido")
            }
            cliente.rutCliente = RutUtil.normalizar(request.rutCliente)
        }
        if (request.nombre?.isNotBlank() == true) cliente.nombre = request.nombre
        if (request.razonSocialCliente?.isNotBlank() == true) cliente.razonSocialCliente = request.razonSocialCliente
        if (request.giroCliente?.isNotBlank() == true) cliente.giroCliente = request.giroCliente
        if (request.direccionCliente?.isNotBlank() == true) cliente.direccionCliente = request.direccionCliente
        if (request.comunaCliente?.isNotBlank() == true) cliente.comunaCliente = request.comunaCliente
        if (request.ciudadCliente?.isNotBlank() == true) cliente.ciudadCliente = request.ciudadCliente
        if (request.emailCliente?.isNotBlank() == true) cliente.emailCliente = request.emailCliente
        if (request.telefonoCliente?.isNotBlank() == true) cliente.telefonoCliente = request.telefonoCliente

        return clienteRepository.save(cliente)
    }

    @Transactional
    fun eliminarCliente(id: Long) = clienteRepository.deleteById(id)
}

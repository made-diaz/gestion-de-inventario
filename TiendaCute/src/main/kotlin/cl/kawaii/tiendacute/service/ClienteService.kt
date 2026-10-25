package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.model.Cliente
import cl.kawaii.tiendacute.repository.ClienteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClienteService(private val clienteRepository: ClienteRepository) {

    @Transactional(readOnly = true)
    fun listarClientes(): List<Cliente> = clienteRepository.findAll()

    @Transactional(readOnly = true)
    fun buscarPorId(id:Long): Cliente = clienteRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Cliente no encontrado con ID: $id") }

    @Transactional
    fun guardarCliente(cliente: Cliente) : Cliente {
        // Es un alta: se ignora cualquier id enviado por el cliente.
        cliente.idCliente = 0
        return clienteRepository.save(cliente)
    }

    @Transactional
    fun eliminarCliente(id: Long) = clienteRepository.deleteById(id)
}
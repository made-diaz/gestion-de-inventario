package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.ClienteActualizarRequest
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.Cliente
import cl.kawaii.tiendacute.repository.ClienteRepository
import cl.kawaii.tiendacute.util.RutUtil
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root

@Service
class ClienteService(private val clienteRepository: ClienteRepository) {

    /**
     * Lista clientes con paginación y filtro opcional `q` por nombre o RUT.
     */
    @Transactional(readOnly = true)
    fun listarClientes(
        pageable: Pageable?,
        q: String?
    ): Page<Cliente> {
        val spec: Specification<Cliente> = Specification { root: Root<Cliente>, query: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()
            if (q != null && q.isNotEmpty()) {
                val like = "%${q.lowercase()}%"
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get<String>("nombre")), like),
                    cb.like(cb.lower(root.get<String>("rutCliente")), like)
                ))
            }
            cb.and(*predicates.toTypedArray())
        }
        return clienteRepository.findAll(spec, pageable ?: Pageable.unpaged())
    }

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

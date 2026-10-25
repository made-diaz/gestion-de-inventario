package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.dto.ClienteActualizarRequest
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.Cliente
import cl.kawaii.tiendacute.repository.ClienteRepository
import cl.kawaii.tiendacute.service.ClienteService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@SpringBootTest
class ClienteActualizarServiceTest {

    @Autowired
    private lateinit var clienteService: ClienteService

    @Autowired
    private lateinit var clienteRepository: ClienteRepository

    @Test
    fun `actualizar cliente con rut valido lo normaliza`() {
        val cliente = clienteRepository.save(Cliente(nombre = "Cliente test"))

        val actualizado = clienteService.actualizarCliente(
            cliente.idCliente,
            ClienteActualizarRequest(rutCliente = "11.111.111-1", nombre = "Nombre nuevo")
        )

        assertEquals("111111111", actualizado.rutCliente)
        assertEquals("Nombre nuevo", actualizado.nombre)
    }

    @Test
    fun `actualizar cliente con rut invalido falla`() {
        val cliente = clienteRepository.save(Cliente(nombre = "Cliente test"))

        assertFailsWith<ReglaNegocioException> {
            clienteService.actualizarCliente(
                cliente.idCliente,
                ClienteActualizarRequest(rutCliente = "12345678-9")
            )
        }
    }

    @Test
    fun `actualizar cliente sin rut no lo toca`() {
        val cliente = clienteRepository.save(Cliente(nombre = "Sin rut", rutCliente = "111111111"))

        clienteService.actualizarCliente(cliente.idCliente, ClienteActualizarRequest(nombre = "Solo nombre"))

        val despues = clienteRepository.findById(cliente.idCliente).orElseThrow()
        assertEquals("111111111", despues.rutCliente)
        assertEquals("Solo nombre", despues.nombre)
    }
}

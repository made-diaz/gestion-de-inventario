package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.PagoRequest
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.MovimientoCaja
import cl.kawaii.tiendacute.model.Pago
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.model.Venta
import cl.kawaii.tiendacute.repository.CajaRepository
import cl.kawaii.tiendacute.repository.MovimientoCajaRepository
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.repository.VentaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class PagoService(
    private val pagoRepository: cl.kawaii.tiendacute.repository.PagoRepository,
    private val ventaRepository: VentaRepository,
    private val cajaRepository: CajaRepository,
    private val movimientoCajaRepository: MovimientoCajaRepository,
    private val usuarioRepository: UsuarioRepository
) {

    @Transactional(readOnly = true)
    fun listarPagos(): List<Pago> = pagoRepository.findAll()

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Pago = pagoRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Pago no encontrado con ID: $id") }

    /**
     * Registra un pago contra una venta en una sola transacción.
     *
     * El monto no puede exceder el saldo pendiente de la venta
     * (total menos lo ya pagado). El monto pagado de la venta se actualiza
     * en la misma transacción, así el saldo nunca se desincroniza.
     *
     * Si hay una caja abierta, el pago deja automáticamente un MovimientoCaja
     * de tipo INGRESO.
     */
    @Transactional
    fun guardarPago(request: PagoRequest): Pago {
        val venta = ventaRepository.findById(request.ventaId)
            .orElseThrow {
                RecursoNoEncontradoException("Venta no encontrada con ID: ${request.ventaId}")
            }

        val saldoPendiente = venta.saldoPendiente()
        if (request.montoPago.compareTo(saldoPendiente) > 0) {
            throw ReglaNegocioException(
                "El monto del pago (${request.montoPago}) excede el saldo pendiente de la venta #${venta.idVenta} ($saldoPendiente)"
            )
        }

        // Actualiza el monto pagado de la venta en la misma transacción.
        venta.montoPagado = venta.montoPagado.add(request.montoPago)
        ventaRepository.save(venta)

        val pago = Pago(
            metodoPago = request.metodoPago,
            montoPago = request.montoPago,
            referencia = request.referencia,
            venta = venta
        )
        val pagoGuardado = pagoRepository.save(pago)

        // Movimiento de caja automático si hay caja abierta.
        registrarMovimientoCaja(
            monto = request.montoPago,
            concepto = "Pago de venta #${venta.idVenta}",
            referenciaTipo = "PAGO",
            referenciaId = pagoGuardado.idPago,
            usuarioId = request.usuarioId
        )

        return pagoGuardado
    }

    @Transactional(readOnly = true)
    fun saldoPendienteDeVenta(ventaId: Long): BigDecimal {
        val venta = ventaRepository.findById(ventaId)
            .orElseThrow {
                RecursoNoEncontradoException("Venta no encontrada con ID: $ventaId")
            }
        return venta.saldoPendiente()
    }

    private fun registrarMovimientoCaja(
        monto: BigDecimal,
        concepto: String,
        referenciaTipo: String?,
        referenciaId: Long?,
        usuarioId: Long
    ) {
        val cajaAbierta = cajaRepository.findFirstByEstadoOrderByIdCajaDesc("ABIERTA") ?: return

        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { RecursoNoEncontradoException("Usuario no encontrado con ID: $usuarioId") }

        movimientoCajaRepository.save(
            MovimientoCaja(
                tipoMovimiento = "INGRESO",
                monto = monto,
                concepto = concepto,
                referenciaTipo = referenciaTipo,
                referenciaId = referenciaId,
                caja = cajaAbierta,
                usuario = usuario
            )
        )
    }

    companion object {
        private const val TIPO_MOVIMIENTO_INGRESO = "INGRESO"
        private const val ESTADO_CAJA_ABIERTA = "ABIERTA"
    }
}

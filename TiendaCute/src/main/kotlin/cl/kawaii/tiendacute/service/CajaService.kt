package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.exception.OperacionNoPermitidaException
import cl.kawaii.tiendacute.model.Caja
import cl.kawaii.tiendacute.model.MovimientoCaja
import cl.kawaii.tiendacute.model.Usuario
import cl.kawaii.tiendacute.repository.CajaRepository
import cl.kawaii.tiendacute.repository.MovimientoCajaRepository
import cl.kawaii.tiendacute.repository.UsuarioRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class CajaService(
    private val cajaRepository: CajaRepository,
    private val movimientoCajaRepository: MovimientoCajaRepository,
    private val usuarioRepository: UsuarioRepository
) {

    private companion object {
        const val ESTADO_ABIERTA = "ABIERTA"
        const val ESTADO_CERRADA = "CERRADA"
        const val TIPO_INGRESO = "INGRESO"
    }

    @Transactional(readOnly = true)
    fun listarCajas(): List<Caja> = cajaRepository.findAll()

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Caja = cajaRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Caja no encontrada con ID: $id") }

    /**
     * Abre una caja con un monto inicial. Solo puede haber una caja abierta a
     * la vez.
     */
    @Transactional
    fun abrirCaja(usuarioId: Long, montoInicial: BigDecimal): Caja {
        if (cajaRepository.existsByEstado(ESTADO_ABIERTA)) {
            throw OperacionNoPermitidaException(
                "Ya hay una caja abierta. Ciérrela antes de abrir otra."
            )
        }
        if (montoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw ReglaNegocioException("El monto inicial no puede ser negativo")
        }

        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow {
                RecursoNoEncontradoException("Usuario no encontrado con ID: $usuarioId")
            }

        return cajaRepository.save(
            Caja(
                montoInicial = montoInicial,
                estado = ESTADO_ABIERTA,
                usuario = usuario
            )
        )
    }

    /**
     * Cierra la caja abierta con el monto final contado.
     */
    @Transactional
    fun cerrarCaja(usuarioId: Long, montoFinal: BigDecimal): Caja {
        val caja = cajaRepository.findFirstByEstadoOrderByIdCajaDesc(ESTADO_ABIERTA)
            ?: throw ReglaNegocioException("No hay una caja abierta para cerrar")

        if (montoFinal.compareTo(BigDecimal.ZERO) < 0) {
            throw ReglaNegocioException("El monto final no puede ser negativo")
        }

        caja.estado = ESTADO_CERRADA
        caja.fechaCierre = LocalDateTime.now()
        caja.montoFinal = montoFinal
        return cajaRepository.save(caja)
    }

    /** Devuelve la caja abierta si existe, o null. */
    @Transactional(readOnly = true)
    fun estadoCaja(): Caja? = cajaRepository.findFirstByEstadoOrderByIdCajaDesc(ESTADO_ABIERTA)

    @Transactional(readOnly = true)
    fun listarMovimientos(): List<MovimientoCaja> = movimientoCajaRepository.findAll()

    /**
     * Registra un movimiento de caja de tipo INGRESO sobre la caja abierta,
     * si existe. Se usa desde VentaService y PagoService para dejar rastro de
     * el dinero que entra por ventas y cobros.
     */
    fun registrarIngresoCaja(
        monto: BigDecimal,
        concepto: String,
        referenciaTipo: String?,
        referenciaId: Long?,
        usuarioId: Long
    ) {
        val cajaAbierta = cajaRepository.findFirstByEstadoOrderByIdCajaDesc(ESTADO_ABIERTA) ?: return

        val usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow { RecursoNoEncontradoException("Usuario no encontrado con ID: $usuarioId") }

        movimientoCajaRepository.save(
            MovimientoCaja(
                tipoMovimiento = TIPO_INGRESO,
                monto = monto,
                concepto = concepto,
                referenciaTipo = referenciaTipo,
                referenciaId = referenciaId,
                caja = cajaAbierta,
                usuario = usuario
            )
        )
    }
}

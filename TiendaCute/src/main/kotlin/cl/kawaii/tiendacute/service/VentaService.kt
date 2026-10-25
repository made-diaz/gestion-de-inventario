package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.VentaRequest
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.DetalleVenta
import cl.kawaii.tiendacute.model.MovimientoStock
import cl.kawaii.tiendacute.model.Venta
import cl.kawaii.tiendacute.repository.ClienteRepository
import cl.kawaii.tiendacute.repository.DetalleVentaRepository
import cl.kawaii.tiendacute.repository.MovimientoStockRepository
import cl.kawaii.tiendacute.repository.ProductoRepository
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.repository.VentaRepository
import cl.kawaii.tiendacute.service.CajaService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Service
class VentaService(
    private val ventaRepository: VentaRepository,
    private val detalleVentaRepository: DetalleVentaRepository,
    private val productoRepository: ProductoRepository,
    private val movimientoStockRepository: MovimientoStockRepository,
    private val usuarioRepository: UsuarioRepository,
    private val clienteRepository: ClienteRepository,
    private val cajaService: CajaService
) {

    /**
     * Lista ventas con paginación y filtros opcionales.
     *
     * Filtros:
     *   - desde: fechaVenta >= desde
     *   - hasta: fechaVenta <= hasta
     *   - idCliente: filtra por cliente exacto
     *   - idUsuario: filtra por usuario exacto
     *   Orden: fecha desc
     */
    @Transactional(readOnly = true)
    fun listarVentas(
        pageable: Pageable?,
        desde: LocalDateTime?,
        hasta: LocalDateTime?,
        idCliente: Long?,
        idUsuario: Long?
    ): Page<Venta> {
        val spec: Specification<Venta> = Specification { root: Root<Venta>, query: CriteriaQuery<*>, cb: CriteriaBuilder ->
            val predicates = mutableListOf<Predicate>()
            if (desde != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get<LocalDateTime>("fechaVenta"), desde))
            }
            if (hasta != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get<LocalDateTime>("fechaVenta"), hasta))
            }
            if (idCliente != null) {
                predicates.add(cb.equal(root.get<Long>("cliente_id"), idCliente))
            }
            if (idUsuario != null) {
                predicates.add(cb.equal(root.get<Long>("usuario_id"), idUsuario))
            }
            query.orderBy(cb.desc(root.get<LocalDateTime>("fechaVenta")))
            cb.and(*predicates.toTypedArray())
        }
        return ventaRepository.findAll(spec, pageable ?: Pageable.unpaged())
    }

    @Transactional(readOnly = true)
    fun buscarPorId(id: Long): Venta = ventaRepository.findById(id)
        .orElseThrow { RecursoNoEncontradoException("Venta no encontrada con id $id") }

    /** ... */
    @Transactional
    fun registrarVenta(request: VentaRequest): Venta {
        if (request.detalles.isEmpty()) {
            throw ReglaNegocioException("La venta debe incluir al menos un producto")
        }

        val usuario = usuarioRepository.findById(request.usuarioId)
            .orElseThrow {
                RecursoNoEncontradoException("Usuario no encontrado con ID: ${request.usuarioId}")
            }

        val venta = Venta(
            tipoDocumento = request.venta.tipoDocumento,
            estadoVenta = request.venta.estadoVenta,
            usuario = usuario
        )

        request.clienteRut?.takeIf { it.isNotBlank() }?.let { rut ->
            venta.cliente = clienteRepository.findByRutCliente(rut)
                .orElseThrow { RecursoNoEncontradoException("Cliente no encontrado con RUT: $rut") }
        }

        val cantidadesPorProducto = request.detalles
            .groupBy({ it.productoId }, { it.cantidad })
            .mapValues { (_, cantidades) -> cantidades.sum() }

        var totalNeto = BigDecimal.ZERO
        val detalles = mutableListOf<DetalleVenta>()

        cantidadesPorProducto.forEach { (productoId, cantidad) ->
            if (cantidad <= 0) {
                throw ReglaNegocioException("La cantidad debe ser mayor a cero para el producto $productoId")
            }

            val producto = productoRepository.findByIdForUpdate(productoId)
                .orElseThrow {
                    RecursoNoEncontradoException("Producto no encontrado con ID: $productoId")
                }

            if (!producto.activo) {
                throw ReglaNegocioException("El producto '${producto.nombreProducto}' no está activo")
            }

            if (producto.stockActualProducto < cantidad) {
                throw ReglaNegocioException(
                    "Stock insuficiente para '${producto.nombreProducto}': " +
                        "disponible ${producto.stockActualProducto}, solicitado $cantidad"
                )
            }

            val precioUnitario = producto.precioVentaProducto
            val subtotal = precioUnitario.multiply(BigDecimal(cantidad))
            totalNeto = totalNeto.add(subtotal)

            producto.stockActualProducto -= cantidad
            productoRepository.save(producto)

            movimientoStockRepository.save(
                MovimientoStock(
                    tipoMovimiento = TIPO_MOVIMIENTO_SALIDA,
                    cantidadMovimiento = cantidad,
                    motivoMovimiento = MOTIVO_VENTA,
                    producto = producto,
                    usuario = usuario
                )
            )

            detalles += DetalleVenta(
                cantidad = cantidad,
                precioUnitario = precioUnitario,
                subtotal = subtotal,
                producto = producto
            )
        }

        val netoRedondeado = totalNeto.setScale(2, RoundingMode.HALF_UP)
        val iva = netoRedondeado.multiply(TASA_IVA).setScale(2, RoundingMode.HALF_UP)

        venta.totalNeto = netoRedondeado
        venta.iva = iva
        venta.total = netoRedondeado.add(iva)

        val ventaGuardada = ventaRepository.save(venta)

        detalles.forEach { detalle ->
            detalle.venta = ventaGuardada
            detalleVentaRepository.save(detalle)
        }

        cajaService.registrarIngresoCaja(
            monto = ventaGuardada.total,
            concepto = "Venta #${ventaGuardada.idVenta}",
            referenciaTipo = "VENTA",
            referenciaId = ventaGuardada.idVenta,
            usuarioId = usuario.idUsuario
        )

        return ventaGuardada
    }

    companion object {
        private val TASA_IVA: BigDecimal = BigDecimal("0.19")
        private const val TIPO_MOVIMIENTO_SALIDA = "SALIDA"
        private const val MOTIVO_VENTA = "VENTA"
    }
}

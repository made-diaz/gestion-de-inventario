package cl.kawaii.tiendacute.service

import cl.kawaii.tiendacute.dto.CajeroDto
import cl.kawaii.tiendacute.dto.ClienteDto
import cl.kawaii.tiendacute.dto.DocumentoDto
import cl.kawaii.tiendacute.dto.LineaVentaDto
import cl.kawaii.tiendacute.dto.PagoDto
import cl.kawaii.tiendacute.dto.PagoVentaRequest
import cl.kawaii.tiendacute.dto.VentaDetalleDto
import cl.kawaii.tiendacute.dto.VentaListDto
import cl.kawaii.tiendacute.dto.VentaRequest
import cl.kawaii.tiendacute.exception.RecursoNoEncontradoException
import cl.kawaii.tiendacute.exception.ReglaNegocioException
import cl.kawaii.tiendacute.model.DetalleVenta
import cl.kawaii.tiendacute.model.MovimientoStock
import cl.kawaii.tiendacute.model.Pago
import cl.kawaii.tiendacute.model.Venta
import cl.kawaii.tiendacute.repository.ClienteRepository
import cl.kawaii.tiendacute.repository.DetalleVentaRepository
import cl.kawaii.tiendacute.repository.DocumentoTributarioRepository
import cl.kawaii.tiendacute.repository.MovimientoStockRepository
import cl.kawaii.tiendacute.repository.PagoRepository
import cl.kawaii.tiendacute.repository.ProductoRepository
import cl.kawaii.tiendacute.repository.UsuarioRepository
import cl.kawaii.tiendacute.repository.VentaRepository
import cl.kawaii.tiendacute.service.CajaService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
    private val pagoRepository: PagoRepository,
    private val documentoTributarioRepository: DocumentoTributarioRepository,
    private val cajaService: CajaService
) {

    /**
     * Listado liviano de ventas con paginación y filtros opcionales.
     * Proyección directa a DTO: sin N+1.
     */
    @Transactional(readOnly = true)
    fun listarVentas(
        pageable: Pageable?,
        desde: LocalDateTime?,
        hasta: LocalDateTime?,
        idCliente: Long?,
        idUsuario: Long?
    ): Page<VentaListDto> {
        return ventaRepository.listarVentasDto(desde, hasta, idCliente, idUsuario, pageable ?: Pageable.unpaged())
            .map { row ->
                VentaListDto(
                    idVenta = row[0] as Long,
                    fechaVenta = row[1] as LocalDateTime?,
                    tipoDocumento = row[2] as String,
                    total = row[3] as BigDecimal,
                    cantidadLineas = row[4] as Long,
                    clienteNombre = row[5] as String?,
                    usuarioNombre = row[6] as String
                )
            }
    }

    /**
     * Detalle completo de una venta con líneas, pagos, cajero, cliente y
     * documento tributario. Los detalles y pagos se cargan por consultas
     * separadas porque Venta no declara propiedades @OneToMany.
     */
    @Transactional(readOnly = true)
    fun buscarDetalle(id: Long): VentaDetalleDto {
        val venta = ventaRepository.findVentaConDetalles(id)
            .orElseThrow { RecursoNoEncontradoException("Venta no encontrada con id $id") }

        val detalles = detalleVentaRepository.findByVenta_IdVenta(id)
        val pagos = pagoRepository.findByVenta_IdVenta(id)
        val documento = documentoTributarioRepository.findByVenta_IdVenta(id)

        return VentaDetalleDto(
            idVenta = venta.idVenta,
            fechaVenta = venta.fechaVenta,
            tipoDocumento = venta.tipoDocumento,
            estadoVenta = venta.estadoVenta,
            totalNeto = venta.totalNeto,
            iva = venta.iva,
            total = venta.total,
            montoPagado = venta.montoPagado,
            saldoPendiente = venta.saldoPendiente(),
            cajero = CajeroDto(id = venta.usuario.idUsuario, nombre = venta.usuario.usuario),
            cliente = venta.cliente?.let { it ->
                ClienteDto(id = it.idCliente, nombre = it.nombre, rut = it.rutCliente)
            },
            documento = documento?.let { it ->
                DocumentoDto(folio = it.folio, estadoSii = it.estadoSii)
            },
            pagos = pagos.map { p ->
                PagoDto(
                    metodoPago = p.metodoPago,
                    montoPago = p.montoPago,
                    referencia = p.referencia,
                    creadoEn = p.creadoEn
                )
            },
            lineas = detalles.map { d ->
                LineaVentaDto(
                    idProducto = d.producto.idProducto,
                    nombreProducto = d.producto.nombreProducto,
                    cantidad = d.cantidad,
                    precioUnitario = d.precioUnitario,
                    subtotal = d.subtotal
                )
            }
        )
    }

    /**
     * Registra una venta completa en una sola transacción, con pagos opcionales.
     *
     * Los pagos se validan contra el total del servidor: la suma de todos los
     * pagos no puede exceder el total de la venta. Cada pago individual no
     * puede exceder el saldo pendiente restante.
     */
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

        // Si el mismo producto viene repetido, se suma la cantidad antes de validar stock.
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

        // Registrar pagos opcionales en la misma transacción, validados contra el total.
        if (request.pagos.isNotEmpty()) {
            registrarPagosDeVenta(ventaGuardada, request.pagos, usuario.idUsuario)
        }

        // Movimiento de caja automático si hay caja abierta (por el total de la venta).
        cajaService.registrarIngresoCaja(
            monto = ventaGuardada.total,
            concepto = "Venta #${ventaGuardada.idVenta}",
            referenciaTipo = "VENTA",
            referenciaId = ventaGuardada.idVenta,
            usuarioId = usuario.idUsuario
        )

        return ventaGuardada
    }

    /**
     * Registra los pagos de una venta, validando que no excedan el saldo pendiente.
     * Actualiza montoPagado de la venta en la misma transacción.
     */
    private fun registrarPagosDeVenta(
        venta: Venta,
        pagos: List<PagoVentaRequest>,
        usuarioId: Long
    ) {
        var saldoPendiente = venta.saldoPendiente()

        pagos.forEach { pagoReq ->
            if (pagoReq.montoPago.compareTo(saldoPendiente) > 0) {
                throw ReglaNegocioException(
                    "El pago de ${pagoReq.montoPago} excede el saldo pendiente de la venta #${venta.idVenta} ($saldoPendiente)"
                )
            }

            val pago = Pago(
                metodoPago = pagoReq.metodoPago,
                montoPago = pagoReq.montoPago,
                referencia = pagoReq.referencia,
                venta = venta
            )
            val pagoGuardado = pagoRepository.save(pago)

            venta.montoPagado = venta.montoPagado + pagoReq.montoPago
            saldoPendiente = saldoPendiente - pagoReq.montoPago

            // Movimiento de caja por el pago.
            cajaService.registrarIngresoCaja(
                monto = pagoReq.montoPago,
                concepto = "Pago de venta #${venta.idVenta}",
                referenciaTipo = "PAGO",
                referenciaId = pagoGuardado.idPago,
                usuarioId = usuarioId
            )
        }

        ventaRepository.save(venta)
    }

    companion object {
        private val TASA_IVA: BigDecimal = BigDecimal("0.19")
        private const val TIPO_MOVIMIENTO_SALIDA = "SALIDA"
        private const val MOTIVO_VENTA = "VENTA"
    }
}

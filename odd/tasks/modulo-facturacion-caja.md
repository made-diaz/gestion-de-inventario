# Feature: facturación, caja y validación de pagos

Objetivo: que una venta se pueda cobrar con pago validado contra su total, que el cobro pase por una caja abierta y que se pueda emitir documento tributario (boleta/factura) solo sobre ventas pagadas.
Rama: `feat/facturacion-caja`. Runner: `.\\gradlew.bat test`.

## Specs

- **S1.** Al registrar un pago, el monto no puede exceder el saldo pendiente de la venta (`total - pagos ya registrados`). Si lo excede → `400`. (Ítem 4)
- **S2.** El pago se registra en una sola transacción con la actualización del monto pagado de la venta. Saldo pendiente = total - montoPagado.
- **S3.** Se admiten pagos parciales (abonos) y pago completo. Una venta queda "pagada" cuando saldo pendiente = 0.
- **S4.** Pago contra venta inexistente → `404`.
- **S5.** Se puede abrir una caja (usuario + monto inicial). Estado `ABIERTA`. (Ítem 3)
- **S6.** Solo puede haber una caja abierta a la vez. Abrir otra mientras haya una abierta → `409`.
- **S7.** Se puede cerrar la caja abierta (monto final contado). Estado `CERRADA`. Cerrar una ya cerrada → `400`.
- **S8.** Cada venta registrada genera automáticamente un MovimientoCaja de tipo `INGRESO` por el total de la venta, si hay caja abierta.
- **S9.** Cada pago registrado genera automáticamente un MovimientoCaja de tipo `INGRESO` por el monto del pago, si hay caja abierta.
- **S10.** Consultar estado de caja: abierta/cerrada, saldos y movimientos.
- **S11.** Se puede emitir un DocumentoTributario (`BOLETA` / `FACTURA`) para una venta. (Ítem 2)
- **S12.** El folio es correlativo automático por tipo de documento; el cliente no lo envía.
- **S13.** Solo se puede emitir si la venta está pagada completa (saldo pendiente = 0). Si no → `400`.
- **S14.** No se puede emitir dos documentos para la misma venta → `409`.
- **S15.** El documento se guarda con estado SII `EMITIDO`, fecha de envío y XML simulado.

## Tasks

- **T1.** Modelos `Caja` y `MovimientoCaja` + repositorios. (S5-S7, S10)
- **T2.** Campo `montoPagado` en `Venta` y método `saldoPendiente()`. (S1-S3)
- **T3.** `PagoRequest` DTO y `PagoService` con validación de saldo + movimiento de caja automático. (S1-S4, S9)
- **T4.** `CajaService` y `CajaController` (abrir, cerrar, estado, movimientos). (S5-S7, S10)
- **T5.** `VentaService` genera MovimientoCaja automático al registrar venta. (S8)
- **T6.** Emisión de `DocumentoTributario`: folio correlativo, validar pago completo, doble emisión, XML. (S11-S15)
- **T7.** Reglas de seguridad para los nuevos endpoints (`/api/cajas/**`, emisión de documentos).
- **T8.** Tests: servicio e integración para los 3 módulos. (hecho)
- **T9.** Build, commit y merge. (en progreso)

## Log

- **L1.** Usuario: "puedes tocar los archivos que no haz creado solo para mejorar el programa... inventarios, emisión de boletas y facturas y registro de caja... primero 2, 3, 4 y luego 1".
- **L2.** Decisión: el pago (ítem 4) se implementa antes que caja (3) y documentos (2) porque es la base de ambos: sin pago validado no hay "venta pagada" ni documento que emitir.
- **L3.** Decisión: caja es una entidad aparte de `MovimientoStock` (que es de productos). Los movimientos de caja rastrean dinero, no stock.
- **L4.** Decisión: el folio del documento tributario es correlativo por tipo en base de datos (`MAX(folio) + 1`). Bajo alta concurrencia habría que moverlo a una tabla de contadores; aceptable para esta etapa.

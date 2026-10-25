package cl.kawaii.tiendacute.dto

import java.math.BigDecimal

data class ProductoActualizarRequest(
    val nombreProducto: String? = null,
    val codigoProducto: String? = null,
    val precioCompraProducto: BigDecimal? = null,
    val precioVentaProducto: BigDecimal? = null,
    val stockActualProducto: Int? = null,
    val activo: Boolean? = null,
    val categoriaId: Long? = null
)

data class CategoriaActualizarRequest(
    val nombreCategoria: String? = null
)

data class ClienteActualizarRequest(
    val rutCliente: String? = null,
    val nombre: String? = null,
    val razonSocialCliente: String? = null,
    val giroCliente: String? = null,
    val direccionCliente: String? = null,
    val comunaCliente: String? = null,
    val ciudadCliente: String? = null,
    val emailCliente: String? = null,
    val telefonoCliente: String? = null
)

data class UsuarioActualizarRequest(
    val usuario: String? = null,
    val rolUsuario: String? = null,
    val activoUsuario: Boolean? = null
)

data class CambiarContrasenaRequest(
    val password: String
)

package cl.kawaii.tiendacute.dto

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "El usuario es obligatorio")
    val usuario: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    val password: String
)

data class LoginResponse(
    val token: String,
    val tipo: String = "Bearer",
    val expiraEnSegundos: Long,
    val usuario: String,
    val rol: String
)

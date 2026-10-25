package cl.kawaii.tiendacute.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "El usuario es obligatorio")
    @field:Size(max = 64, message = "El usuario no puede superar los 64 caracteres")
    val usuario: String,

    @field:NotBlank(message = "La contraseña es obligatoria")
    @field:Size(max = 128, message = "La contraseña no puede superar los 128 caracteres")
    val password: String
)

data class LoginResponse(
    val token: String,
    val tipo: String = "Bearer",
    val expiraEnSegundos: Long,
    val usuario: String,
    val rol: String
)

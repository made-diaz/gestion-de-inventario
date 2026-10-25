package cl.kawaii.tiendacute.controller

import cl.kawaii.tiendacute.dto.LoginRequest
import cl.kawaii.tiendacute.dto.LoginResponse
import cl.kawaii.tiendacute.service.AuthService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): LoginResponse =
        authService.login(request)
}

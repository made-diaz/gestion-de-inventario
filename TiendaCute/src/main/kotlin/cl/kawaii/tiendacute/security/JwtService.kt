package cl.kawaii.tiendacute.security

import cl.kawaii.tiendacute.model.Usuario
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") secreto: String,
    @Value("\${app.jwt.expiracion-minutos:120}") private val expiracionMinutos: Long
) {

    private val clave: SecretKey

    init {
        val bytes = secreto.toByteArray(Charsets.UTF_8)
        require(bytes.size >= 32) {
            "app.jwt.secret debe tener al menos 32 caracteres (256 bits) para firmar con HS256. " +
                "Definilo en env.properties como JWT_SECRET."
        }
        clave = Keys.hmacShaKeyFor(bytes)
    }

    fun generarToken(usuario: Usuario): String {
        val ahora = Instant.now()
        val expiracion = ahora.plusSeconds(expiracionEnSegundos())

        return Jwts.builder()
            .subject(usuario.usuario)
            .claim("rol", usuario.rolUsuario)
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(expiracion))
            .signWith(clave, Jwts.SIG.HS256)
            .compact()
    }

    fun extraerUsuario(token: String): String =
        leerClaims(token).subject

    fun esValido(token: String, usuario: String): Boolean = try {
        val claims = leerClaims(token)
        claims.subject == usuario && claims.expiration?.after(Date()) == true
    } catch (e: JwtException) {
        false
    } catch (e: IllegalArgumentException) {
        false
    }

    fun expiracionEnSegundos(): Long = expiracionMinutos * 60

    private fun leerClaims(token: String) =
        Jwts.parser().verifyWith(clave).build().parseSignedClaims(token).payload
}

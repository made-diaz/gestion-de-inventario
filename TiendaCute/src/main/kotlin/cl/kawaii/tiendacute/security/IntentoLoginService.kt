package cl.kawaii.tiendacute.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Frena la fuerza bruta sobre el login contando fallos por usuario + IP.
 *
 * Es estado en memoria: si mañana hay más de una instancia, esto se mueve a
 * Redis o a la base. Hoy alcanza y no agrega infraestructura.
 */
@Service
class IntentoLoginService(
    private val reloj: Clock,
    @Value("\${app.login.max-intentos:5}") private val maxIntentos: Int,
    @Value("\${app.login.bloqueo-minutos:15}") private val bloqueoMinutos: Long
) {

    private class Registro {
        var fallos: Int = 0
        var bloqueadoHasta: Instant? = null
    }

    private val registros = ConcurrentHashMap<String, Registro>()

    fun estaBloqueado(clave: String): Boolean {
        val registro = registros[clave] ?: return false
        val hasta = registro.bloqueadoHasta ?: return false

        if (reloj.instant().isBefore(hasta)) return true

        // La ventana expiró: se descarta el registro y se vuelve a empezar.
        registros.remove(clave)
        return false
    }

    fun registrarFallo(clave: String) {
        val registro = registros.computeIfAbsent(clave) { Registro() }
        synchronized(registro) {
            registro.fallos++
            if (registro.fallos >= maxIntentos) {
                registro.bloqueadoHasta = reloj.instant().plus(Duration.ofMinutes(bloqueoMinutos))
            }
        }
    }

    fun limpiar(clave: String) {
        registros.remove(clave)
    }

    fun intentosRestantes(clave: String): Int =
        maxIntentos - (registros[clave]?.fallos ?: 0)

    fun bloqueoEnSegundos(): Long = Duration.ofMinutes(bloqueoMinutos).seconds
}

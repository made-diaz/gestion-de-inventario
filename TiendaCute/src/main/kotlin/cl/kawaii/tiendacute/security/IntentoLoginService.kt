package cl.kawaii.tiendacute.security

import cl.kawaii.tiendacute.exception.DemasiadosIntentosException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Frena la fuerza bruta sobre el login contando intentos por usuario + IP.
 *
 * El intento se reserva ANTES de verificar la contraseña. Si se contara
 * después, una ráfaga de requests en paralelo pasaría todas juntas por el
 * mismo hueco entre "consulto el contador" y "verifico": diez requests
 * simultáneos daban diez oportunidades reales con el límite en cinco.
 *
 * Es estado en memoria: si mañana hay más de una instancia, esto se mueve a
 * Redis o a la base.
 */
@Service
class IntentoLoginService(
    private val reloj: Clock,
    @Value("\${app.login.max-intentos:5}") private val maxIntentos: Int,
    @Value("\${app.login.bloqueo-minutos:15}") private val bloqueoMinutos: Long
) {

    private class Registro {
        var intentos: Int = 0
        var bloqueadoHasta: Instant? = null
    }

    private val registros = ConcurrentHashMap<String, Registro>()

    /**
     * Suma un intento para la clave y lanza [DemasiadosIntentosException] si ya
     * estaba bloqueada o si este intento supera el máximo permitido.
     *
     * El bloqueo se aplica acá, no después de autenticar: así el atacante no
     * gasta intentos de BCrypt ni puede colar requests concurrentes.
     */
    fun reservarIntento(clave: String) {
        val registro = registros.computeIfAbsent(clave) { Registro() }

        synchronized(registro) {
            val ahora = reloj.instant()

            val hasta = registro.bloqueadoHasta
            if (hasta != null) {
                if (ahora.isBefore(hasta)) throw bloqueado()

                // La ventana expiró: se vuelve a empezar.
                registro.bloqueadoHasta = null
                registro.intentos = 0
            }

            registro.intentos++
            if (registro.intentos > maxIntentos) {
                registro.bloqueadoHasta = ahora.plus(Duration.ofMinutes(bloqueoMinutos))
                throw bloqueado()
            }
        }

        acotarMemoria()
    }

    fun estaBloqueado(clave: String): Boolean {
        val registro = registros[clave] ?: return false
        val hasta = registro.bloqueadoHasta ?: return false

        if (reloj.instant().isBefore(hasta)) return true

        registros.remove(clave)
        return false
    }

    fun limpiar(clave: String) {
        registros.remove(clave)
    }

    fun intentosRestantes(clave: String): Int =
        (maxIntentos - (registros[clave]?.intentos ?: 0)).coerceAtLeast(0)

    fun bloqueoEnSegundos(): Long = Duration.ofMinutes(bloqueoMinutos).seconds

    private fun bloqueado() = DemasiadosIntentosException(
        bloqueoEnSegundos(),
        "Demasiados intentos fallidos. Esperá unos minutos antes de reintentar."
    )

    /**
     * Un atacante puede mandar miles de nombres de usuario distintos para
     * inflar el mapa. Al pasar el tope se descartan los registros que no están
     * bloqueados: los bloqueos activos se conservan.
     */
    private fun acotarMemoria() {
        if (registros.size <= MAXIMO_REGISTROS) return

        val ahora = reloj.instant()
        registros.entries.removeIf { (_, registro) ->
            synchronized(registro) {
                registro.bloqueadoHasta?.let { !ahora.isBefore(it) } ?: true
            }
        }
    }

    private companion object {
        const val MAXIMO_REGISTROS = 10_000
    }
}

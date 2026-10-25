package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.security.IntentoLoginService
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * El bloqueo por fuerza bruta se prueba moviendo el reloj, no esperando 15
 * minutos reales.
 */
class IntentoLoginServiceTest {

    private val inicio = Instant.parse("2026-01-01T10:00:00Z")

    private fun servicio(reloj: Clock) = IntentoLoginService(reloj, maxIntentos = 3, bloqueoMinutos = 15)

    @Test
    fun `bloquea al llegar al maximo de intentos`() {
        val reloj = Clock.fixed(inicio, ZoneOffset.UTC)
        val servicio = servicio(reloj)

        repeat(2) { servicio.registrarFallo("ana|1.1.1.1") }
        assertFalse(servicio.estaBloqueado("ana|1.1.1.1"), "Con 2 de 3 intentos no debe bloquear")

        servicio.registrarFallo("ana|1.1.1.1")
        assertTrue(servicio.estaBloqueado("ana|1.1.1.1"), "Al llegar al máximo debe bloquear")
    }

    @Test
    fun `el bloqueo se libera al pasar la ventana`() {
        val mutable = RelojMutable(inicio)
        val servicio = servicio(mutable)

        repeat(3) { servicio.registrarFallo("beto|2.2.2.2") }
        assertTrue(servicio.estaBloqueado("beto|2.2.2.2"))

        mutable.avanzar(Duration.ofMinutes(16))
        assertFalse(servicio.estaBloqueado("beto|2.2.2.2"), "Pasada la ventana debe liberarse")
    }

    @Test
    fun `un login exitoso limpia el historial`() {
        val servicio = servicio(Clock.fixed(inicio, ZoneOffset.UTC))

        repeat(2) { servicio.registrarFallo("carla|3.3.3.3") }
        servicio.limpiar("carla|3.3.3.3")

        servicio.registrarFallo("carla|3.3.3.3")
        assertFalse(servicio.estaBloqueado("carla|3.3.3.3"), "El contador debía quedar en cero")
    }

    @Test
    fun `el bloqueo es por usuario y direccion`() {
        val servicio = servicio(Clock.fixed(inicio, ZoneOffset.UTC))

        repeat(3) { servicio.registrarFallo("ana|1.1.1.1") }

        assertTrue(servicio.estaBloqueado("ana|1.1.1.1"))
        assertFalse(servicio.estaBloqueado("ana|9.9.9.9"), "Otra IP no debe heredar el bloqueo")
    }

    private class RelojMutable(inicio: Instant) : Clock() {
        private var actual: Instant = inicio
        fun avanzar(cuanto: Duration) {
            actual = actual.plus(cuanto)
        }

        override fun getZone(): ZoneOffset = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId): Clock = this
        override fun instant(): Instant = actual
    }
}

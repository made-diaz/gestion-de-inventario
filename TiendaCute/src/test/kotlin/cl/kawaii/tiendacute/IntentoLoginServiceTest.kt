package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.exception.DemasiadosIntentosException
import cl.kawaii.tiendacute.security.IntentoLoginService
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * El bloqueo por fuerza bruta se prueba moviendo el reloj, no esperando 15
 * minutos reales.
 */
class IntentoLoginServiceTest {

    private val inicio = Instant.parse("2026-01-01T10:00:00Z")

    private fun servicio(reloj: Clock) =
        IntentoLoginService(reloj, maxIntentos = 3, bloqueoMinutos = 15)

    @Test
    fun `permite el maximo de intentos y bloquea el siguiente`() {
        val servicio = servicio(Clock.fixed(inicio, ZoneOffset.UTC))

        repeat(3) { servicio.reservarIntento("ana|1.1.1.1") }
        assertFalse(servicio.estaBloqueado("ana|1.1.1.1"), "Con 3 de 3 intentos todavía no debe bloquear")

        assertFailsWith<DemasiadosIntentosException> { servicio.reservarIntento("ana|1.1.1.1") }
        assertTrue(servicio.estaBloqueado("ana|1.1.1.1"), "El intento que supera el máximo debe bloquear")
    }

    @Test
    fun `el bloqueo se libera al pasar la ventana`() {
        val mutable = RelojMutable(inicio)
        val servicio = servicio(mutable)

        repeat(3) { servicio.reservarIntento("beto|2.2.2.2") }
        assertFailsWith<DemasiadosIntentosException> { servicio.reservarIntento("beto|2.2.2.2") }
        assertTrue(servicio.estaBloqueado("beto|2.2.2.2"))

        mutable.avanzar(Duration.ofMinutes(16))
        assertFalse(servicio.estaBloqueado("beto|2.2.2.2"), "Pasada la ventana debe liberarse")

        // Vuelve a permitir el máximo completo.
        repeat(3) { servicio.reservarIntento("beto|2.2.2.2") }
        assertFalse(servicio.estaBloqueado("beto|2.2.2.2"))
    }

    @Test
    fun `un login exitoso limpia el historial`() {
        val servicio = servicio(Clock.fixed(inicio, ZoneOffset.UTC))

        repeat(2) { servicio.reservarIntento("carla|3.3.3.3") }
        servicio.limpiar("carla|3.3.3.3")

        repeat(3) { servicio.reservarIntento("carla|3.3.3.3") }
        assertFalse(servicio.estaBloqueado("carla|3.3.3.3"), "El contador debía quedar en cero")
    }

    @Test
    fun `el bloqueo es por usuario y direccion`() {
        val servicio = servicio(Clock.fixed(inicio, ZoneOffset.UTC))

        repeat(3) { servicio.reservarIntento("ana|1.1.1.1") }
        assertFailsWith<DemasiadosIntentosException> { servicio.reservarIntento("ana|1.1.1.1") }

        assertTrue(servicio.estaBloqueado("ana|1.1.1.1"))
        assertFalse(servicio.estaBloqueado("ana|9.9.9.9"), "Otra IP no debe heredar el bloqueo")
    }

    @Test
    fun `una rafaga concurrente no entrega mas intentos que el maximo`() {
        val servicio = servicio(Clock.fixed(inicio, ZoneOffset.UTC))
        val largada = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(10)

        val futuros = (1..10).map {
            pool.submit(
                Callable {
                    largada.await()
                    try {
                        servicio.reservarIntento("rafaga|4.4.4.4")
                        true
                    } catch (e: DemasiadosIntentosException) {
                        false
                    }
                }
            )
        }

        largada.countDown()
        val permitidos = futuros.count { it.get(10, TimeUnit.SECONDS) }
        pool.shutdown()

        assertEquals(3, permitidos, "Diez requests en paralelo no pueden conseguir más que el máximo")
    }

    private class RelojMutable(inicio: Instant) : Clock() {
        private var actual: Instant = inicio

        fun avanzar(cuanto: Duration) {
            actual = actual.plus(cuanto)
        }

        override fun getZone(): ZoneOffset = ZoneOffset.UTC
        override fun withZone(zone: ZoneId): Clock = this
        override fun instant(): Instant = actual
    }
}

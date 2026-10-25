package cl.kawaii.tiendacute

import cl.kawaii.tiendacute.util.RutUtil
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RutUtilTest {

    @Test
    fun `valida rut con guion y punto`() {
        assertTrue(RutUtil.validar("11.111.111-1"))
    }

    @Test
    fun `valida rut sin puntos ni guion`() {
        assertTrue(RutUtil.validar("123456785"))
    }

    @Test
    fun `valida rut con guion sin puntos`() {
        assertTrue(RutUtil.validar("12345678-5"))
    }

    @Test
    fun `rechaza rut con dv incorrecto`() {
        assertFalse(RutUtil.validar("12345678-9"))
    }

    @Test
    fun `rechaza rut muy corto`() {
        assertFalse(RutUtil.validar("123"))
    }

    @Test
    fun `rut vacio es valido (campo opcional)`() {
        assertTrue(RutUtil.validar(null))
        assertTrue(RutUtil.validar(""))
        assertTrue(RutUtil.validar("   "))
    }

    @Test
    fun `normaliza elimina puntos y guion`() {
        assertEquals("111111111", RutUtil.normalizar("11.111.111-1"))
        assertEquals("123456789", RutUtil.normalizar("12345678-9"))
        assertNull(RutUtil.normalizar(null))
    }
}

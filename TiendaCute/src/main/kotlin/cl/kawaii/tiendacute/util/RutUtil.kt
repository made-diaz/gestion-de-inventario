package cl.kawaii.tiendacute.util

/**
 * Utilidad para validar RUT chileno (solo número y dígito verificador, sin puntos).
 * Formatos aceptados: "12345678-9", "123456789", "12.345.678-9".
 * El dígito verificador puede ser "K" o "k".
 */
object RutUtil {

    fun validar(rut: String?): Boolean {
        if (rut.isNullOrBlank()) return true // campo opcional

        val limpio = rut.trim().replace(".", "").replace("-", "").uppercase()
        if (limpio.length < 2) return false

        val cuerpo = limpio.substring(0, limpio.length - 1)
        val dv = limpio.substring(limpio.length - 1)
        if (!cuerpo.all { it.isDigit() }) return false

        var factor = 2
        var suma = 0
        for (i in cuerpo.indices.reversed()) {
            suma += cuerpo[i].digitToInt() * factor
            factor = if (factor == 7) 2 else factor + 1
        }

        val dvEsperado = when (11 - (suma % 11)) {
            11 -> "0"
            10 -> "K"
            else -> (11 - (suma % 11)).toString()
        }

        return dv.equals(dvEsperado, ignoreCase = true)
    }

    fun normalizar(rut: String?): String? {
        if (rut.isNullOrBlank()) return null
        return rut.trim().replace(".", "").replace("-", "").uppercase()
    }
}

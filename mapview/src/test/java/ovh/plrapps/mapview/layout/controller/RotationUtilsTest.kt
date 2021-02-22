package ovh.plrapps.mapview.layout.controller

import ovh.plrapps.mapview.util.modulo
import org.junit.Assert.assertEquals
import org.junit.Test

class RotationUtilsTest {
    @Test
    fun moduloTest() {
        val angle = 365f
        assertEquals(5f, angle.modulo())

        val angle2 = -5f
        assertEquals(355f, angle2.modulo())

        val angle3 = -190f
        assertEquals(170f, angle3.modulo())
    }

    @Test
    fun addModuloTest() {
        val angle = 350f
        val new = angle + 15f
        assertEquals(5f, new.modulo())
    }
}
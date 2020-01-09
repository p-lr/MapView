package com.peterlaurence.mapview.layout.controller

import com.peterlaurence.mapview.util.addModulo
import com.peterlaurence.mapview.util.modulo
import org.junit.Assert.assertEquals
import org.junit.Test

class RotationUtilsTest {
    @Test
    fun moduloTest() {
        val angle = 185f
        assertEquals(5f, angle.modulo())

        val angle2 = -5f
        assertEquals(175f, angle2.modulo())

        val angle3 = -190f
        assertEquals(170f, angle3.modulo())
    }

    @Test
    fun addModuloTest() {
        val angle = 170f
        val new = angle.addModulo(15f)
        assertEquals(5f, new)
    }
}
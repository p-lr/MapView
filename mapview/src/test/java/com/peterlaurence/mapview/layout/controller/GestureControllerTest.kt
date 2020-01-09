package com.peterlaurence.mapview.layout.controller

import org.junit.Test

class VisibleAreaTest {
    @Test
    fun rotationTest() {
        val area = VisibleArea(VisibleArea.Corner(0f, 0f), 100, 200)
        area.rotate(90f, 50f, 100f)
        area.rotate(-90f, 50f, 100f)
        println(area.getRect())
    }
}
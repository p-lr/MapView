package com.peterlaurence.mapview.core

import android.graphics.Paint
import java.util.*

/**
 * A pool of [Paint] which has a limited size.
 */
class PaintPool {
    private val pool = LinkedList<Paint>()
    private var size: Int = 0
    private val threshold = 100

    fun getPaint(): Paint? {
        return if (pool.isNotEmpty()) {
            size--
            pool.removeFirst()
        } else {
            null
        }
    }

    fun putPaint(p: Paint) {
        if (size < threshold) {
            size++
            pool.add(p)
        }
    }
}
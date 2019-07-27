package com.peterlaurence.mapview.core

import com.peterlaurence.mapview.util.scale

class CoordinateTranslater(val baseWidth: Int, val baseHeight: Int, val left: Double, val top: Double,
                           val right: Double, val bottom: Double) {

    private val diffX = right - left
    private val diffY = bottom - top

    fun translateX(x: Double): Int {
        val factor = (x - left) / diffX
        return scale(baseWidth, factor.toFloat())
    }

    fun translateY(y: Double): Int {
        val factor = (y - top) / diffY
        return scale(baseHeight, factor.toFloat())
    }

    fun translateAndScaleX(x: Double, scale: Float): Int {
        return scale(translateX(x), scale)
    }

    fun translateAndScaleY(y: Double, scale: Float): Int {
        return scale(translateY(y), scale)
    }

    fun translateAbsoluteToRelativeX(x: Int): Double {
        return left + (x * diffX / baseWidth)
    }

    fun translateAbsoluteToRelativeY(y: Int): Double {
        return top + (y * diffY / baseHeight)
    }

    fun translateAndScaleAbsoluteToRelativeX(x: Int, scale: Float): Double {
        return translateAbsoluteToRelativeX((x / scale).toInt())
    }

    fun translateAndScaleAbsoluteToRelativeY(y: Int, scale: Float): Double {
        return translateAbsoluteToRelativeY((y / scale).toInt())
    }
}
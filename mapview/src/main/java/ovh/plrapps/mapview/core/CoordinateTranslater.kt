package ovh.plrapps.mapview.core

import ovh.plrapps.mapview.ReferentialData
import ovh.plrapps.mapview.util.rotateCenteredX
import ovh.plrapps.mapview.util.rotateCenteredY
import ovh.plrapps.mapview.util.scale
import ovh.plrapps.mapview.util.toRad

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

    /**
     * Apply the reverse rotation operation specified by the [ReferentialData], which has a center
     * and an angle alpha. This return the X coordinate by applying a rotation of -alpha around the
     * center.
     */
    @Suppress("unused")
    fun reverseRotationX(rd: ReferentialData, x: Float, y: Float): Double {
        val centerX = rd.centerX * baseWidth * rd.scale
        val centerY = rd.centerY * baseHeight * rd.scale
        return rotateCenteredX(x, y, centerX, centerY, -rd.angle.toRad())
    }

    /**
     * Apply the reverse rotation operation specified by the [ReferentialData], which has a center
     * and an angle alpha. This return the Y coordinate by applying a rotation of -alpha around the
     * center.
     */
    @Suppress("unused")
    fun reverseRotationY(rd: ReferentialData, x: Float, y: Float): Double {
        val centerX = rd.centerX * baseWidth * rd.scale
        val centerY = rd.centerY * baseHeight * rd.scale
        return rotateCenteredY(x, y, centerX, centerY, -rd.angle.toRad())
    }
}
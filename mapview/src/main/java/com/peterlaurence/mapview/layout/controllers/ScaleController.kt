package com.peterlaurence.mapview.layout.controllers

import com.peterlaurence.mapview.layout.GestureLayout
import kotlin.math.max
import kotlin.math.min

/**
 * Controls the scale of the [GestureLayout], and the scale configuration.
 *
 * @author P.Laurence on 13/12/19
 */
class ScaleController(private val layout: GestureLayout) {
    private var minScale = Float.MIN_VALUE
    private var maxScale = 1f
    private var effectiveMinScale = 0f
    private var minimumScaleMode = MinimumScaleMode.FIT

    private var shouldLoopScale = true

    /**
     * Determines whether the [GestureLayout] should go back to minimum scale after a double-tap at
     * maximum scale.
     *
     * @param shouldLoopScale True to allow going back to minimum scale, false otherwise.
     */
    fun setShouldLoopScale(shouldLoopScale: Boolean) {
        this.shouldLoopScale = shouldLoopScale
    }

    /**
     * Set minimum and maximum mScale values for this layout.
     * Note that if minimumScaleMode is set to [MinimumScaleMode.FIT] or [MinimumScaleMode.FILL], the minimum value set here will be ignored
     * Default values are 0 and 1.
     *
     * @param min Minimum scale the [GestureLayout] should accept.
     * @param max Maximum scale the [GestureLayout] should accept.
     */
    fun setScaleLimits(min: Float, max: Float) {
        minScale = min
        maxScale = max
    }

    fun setMinScale(min: Float) {
        minScale = min
    }

    fun setMinimumScaleMode(minimumScaleMode: MinimumScaleMode) {
        this.minimumScaleMode = minimumScaleMode
        notifyShapeChanged()
    }

    fun getConstrainedDestinationScale(scale: Float): Float {
        var scaleTmp = scale
        scaleTmp = max(scaleTmp, effectiveMinScale)
        scaleTmp = min(scaleTmp, maxScale)
        return scaleTmp
    }

    fun getDoubleTapDestinationScale(scaleDest: Float, scale: Float) : Float {
        val effectiveDestination = if (shouldLoopScale && scale >= maxScale) minScale else scaleDest
        return getConstrainedDestinationScale(effectiveDestination)
    }

    fun notifyShapeChanged() {
        val viewportWidth = layout.width
        val viewportHeight = layout.height
        val baseWidth = layout.baseWidth
        val baseHeight = layout.baseHeight

        calculateMinimumScaleToFit(viewportWidth, viewportHeight, baseWidth, baseHeight)
    }

    private fun calculatedMinScale(minimumScaleX: Float, minimumScaleY: Float): Float {
        return when (minimumScaleMode) {
            MinimumScaleMode.FILL -> max(minimumScaleX, minimumScaleY)
            MinimumScaleMode.FIT -> min(minimumScaleX, minimumScaleY)
            MinimumScaleMode.NONE -> minScale
        }
    }

    private fun calculateMinimumScaleToFit(viewportWidth: Int, viewportHeight: Int, baseWidth: Int, baseHeight: Int) {
        val mMinimumScaleX = viewportWidth / baseWidth.toFloat()
        val mMinimumScaleY = viewportHeight / baseHeight.toFloat()
        val recalculatedMinScale = calculatedMinScale(mMinimumScaleX, mMinimumScaleY)
        if (recalculatedMinScale != effectiveMinScale) {
            effectiveMinScale = recalculatedMinScale
            if (layout.scale < effectiveMinScale) {
                layout.scale = effectiveMinScale
            }
        }
    }

    enum class MinimumScaleMode {
        /**
         * Limit the minimum scale to no less than what
         * would be required to fill the container
         */
        FILL,

        /**
         * Limit the minimum scale to no less than what
         * would be required to fit inside the container
         */
        FIT,

        /**
         * Allow arbitrary minimum scale.
         */
        NONE
    }
}
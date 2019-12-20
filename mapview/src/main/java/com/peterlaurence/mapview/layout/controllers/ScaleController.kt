package com.peterlaurence.mapview.layout.controllers

import com.peterlaurence.mapview.api.MinimumScaleMode
import com.peterlaurence.mapview.util.scale
import kotlin.math.max
import kotlin.math.min

/**
 * Controls the scale of the [Scalable], and the scale configuration.
 *
 * @author P.Laurence on 13/12/19
 */
internal class ScaleController(private val scalable: Scalable) {
    private var minScale = Float.MIN_VALUE
    private var maxScale = 1f
    private var effectiveMinScale = 0f
    private var minimumScaleMode = MinimumScaleMode.FIT

    private var shouldLoopScale = true

    /**
     * The base (not scaled) width of the underlying composite image.
     */
    var baseWidth: Int = 0
        private set
    /**
     * The base (not scaled) height of the underlying composite image.
     */
    var baseHeight: Int = 0
        private set

    /**
     * The scaled width of the underlying composite image.
     */
    var scaledWidth: Int = 0
        private set
    /**
     * The scaled height of the underlying composite image.
     */
    var scaledHeight: Int = 0
        private set

    /**
     * Getter and setter of the scale property.
     * The [ScaleController] is the actual owner of the scale.
     */
    var scale = 1f
        set(scale) {
            val scaleTmp = getConstrainedDestinationScale(scale)
            if (this.scale != scaleTmp) {
                val previous = this.scale
                field = scaleTmp
                updateScaledDimensions()
                scalable.constrainScrollToLimits()
                scalable.onScaleChanged(scaleTmp, previous)
                scalable.onContentChanged()
            }
        }

    private fun updateScaledDimensions() {
        scaledWidth = scale(baseWidth, scale)
        scaledHeight = scale(baseHeight, scale)
    }

    /**
     * Sets the size (width and height) of the layout as it should be rendered at a scale of
     * 1f (100%).
     *
     * @param width  Width of the underlying image, not the view or viewport.
     * @param height Height of the underlying image, not the view or viewport.
     */
    fun setSize(width: Int, height: Int) {
        baseWidth = width
        baseHeight = height
        updateScaledDimensions()
        scalable.onMinScaleUpdateRequest()
        scalable.constrainScrollToLimits()
        scalable.onLayoutChanged()
    }

    /**
     * Determines whether the [Scalable] should go back to minimum scale after a double-tap at
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
     * @param min Minimum scale the [Scalable] should accept.
     * @param max Maximum scale the [Scalable] should accept.
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
        scalable.onMinScaleUpdateRequest()
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

    private fun calculatedMinScale(minimumScaleX: Float, minimumScaleY: Float): Float {
        return when (minimumScaleMode) {
            MinimumScaleMode.FILL -> max(minimumScaleX, minimumScaleY)
            MinimumScaleMode.FIT -> min(minimumScaleX, minimumScaleY)
            MinimumScaleMode.NONE -> minScale
        }
    }

    fun calculateMinimumScaleToFit(viewportWidth: Int, viewportHeight: Int, baseWidth: Int, baseHeight: Int) {
        val mMinimumScaleX = viewportWidth / baseWidth.toFloat()
        val mMinimumScaleY = viewportHeight / baseHeight.toFloat()
        val recalculatedMinScale = calculatedMinScale(mMinimumScaleX, mMinimumScaleY)
        if (recalculatedMinScale != effectiveMinScale) {
            effectiveMinScale = recalculatedMinScale
            if (scale < effectiveMinScale) {
                scale = effectiveMinScale
            }
        }
    }

    interface Scalable {
        fun onMinScaleUpdateRequest()
        fun onLayoutChanged()
        fun onContentChanged()
        fun onScaleChanged(currentScale: Float, previousScale: Float)
        fun constrainScrollToLimits()
    }
}
package com.peterlaurence.mapview.layout.controllers

import com.peterlaurence.mapview.api.MinimumScaleMode
import com.peterlaurence.mapview.layout.controllers.GestureController.Scalable
import com.peterlaurence.mapview.util.scale
import com.peterlaurence.mapview.util.toRad
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Controls the scale of the [Scalable], and the scale configuration.
 *
 * @author P.Laurence on 13/12/19
 */
internal class GestureController(private val scalable: Scalable) {
    private var minScale = Float.MIN_VALUE
    private var maxScale = 1f
    private var effectiveMinScale = 0f
    private var minimumScaleMode = MinimumScaleMode.FIT

    private var shouldLoopScale = true

    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    private var basePadding: Int = 0
    private var scaledPadding: Int = 0

    internal val scrollLimitX: Int
        get() = scaledWidth - screenWidth + scaledPadding

    internal val scrollLimitY: Int
        get() = scaledHeight - screenHeight + scaledPadding

    internal val scrollMinX: Int
        get() = -scaledPadding

    internal val scrollMinY: Int
        get() = -scaledPadding

    private val scrollPosition = ScrollPosition(0, 0)
    private val offsetDestination = OffsetDestination(0, 0, 0f)

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
     * The [GestureController] is the actual owner of the scale.
     */
    var scale = 1f
        set(scale) {
            val scaleTmp = getConstrainedDestinationScale(scale)
            if (this.scale != scaleTmp) {
                val previous = this.scale
                field = scaleTmp
                updateScaledDimensions()
                scalable.constrainScrollToLimits()
                recalculateScaledPadding()
                scalable.onScaleChanged(scaleTmp, previous)
                scalable.onContentChanged()
            }
        }

    private var visibleArea = VisibleArea(VisibleArea.Corner(0f, 0f), 0, 0)

    fun onRotate(rotationDelta: Float, focusX: Float, focusY: Float) {
        visibleArea.rotate(rotationDelta, focusX, focusY)

        println("rotate $rotationDelta ($focusX ; $focusY)")
        println("rect: ${visibleArea.getRect()}")
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

    fun getDoubleTapDestinationScale(scaleDest: Float, scale: Float): Float {
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

    fun getConstrainedScroll(x: Int, y: Int): ScrollPosition {
        scrollPosition.x = scrollMinX.coerceAtLeast(min(x, scrollLimitX))
        scrollPosition.y = scrollMinY.coerceAtLeast(min(y, scrollLimitY))
        return scrollPosition
    }

    fun getOffsetDestination(offsetX: Int, offsetY: Int, destScale: Float): OffsetDestination {
        val destScaleCst = getConstrainedDestinationScale(destScale)
        val deltaScale = destScaleCst / scale
        offsetDestination.scale = destScaleCst

        val scrollX = scrollPosition.x + offsetX
        offsetDestination.x = (scrollX * deltaScale).toInt() - offsetX

        val scrollY = scrollPosition.y + offsetY
        offsetDestination.y = (scrollY * deltaScale).toInt() - offsetY
        return offsetDestination
    }

    /**
     * Adds extra padding around the map, making it possible to scroll past the end of the border
     * even when zoomed in.
     */
    fun setBasePadding(padding: Int) {
        basePadding = padding
        recalculateScaledPadding()
    }

    fun setScreenDimensions(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
    }

    private fun recalculateScaledPadding() {
        scaledPadding = scale(basePadding, scale)
    }

    interface Scalable {
        fun onMinScaleUpdateRequest()
        fun onLayoutChanged()
        fun onContentChanged()
        fun onScaleChanged(currentScale: Float, previousScale: Float)
        fun constrainScrollToLimits()
    }

    /**
     * The scroll position is meant to be unique. To avoid creating a lot of objects of this type,
     * we use mutable properties. The usage of this class is meant to be confined to the UI thread.
     */
    data class ScrollPosition(var x: Int, var y: Int)

    data class OffsetDestination(var x: Int, var y: Int, var scale: Float)
}

/**
 * Expected invariant: [angle] should always have values between 0f anf 180f.
 */
internal class VisibleArea(topLeftCorner: Corner, screenWidth: Int, screenHeight: Int) {
    internal data class Corner(var x: Float, var y: Float)
    internal data class Rectangle(val corner1: Corner, val corner2: Corner, val corner3: Corner, val corner4: Corner)

    private var angle: AngleDegree = 0f

    private val rect = Rectangle(
            topLeftCorner,
            Corner(topLeftCorner.x + screenWidth, topLeftCorner.y),
            Corner(topLeftCorner.x + screenWidth, topLeftCorner.y + screenHeight),
            Corner(topLeftCorner.x, topLeftCorner.y + screenHeight))

//    fun moveTo(x: Int, y: Int) {
//        rect.corner1.shift(x.toFloat(), y.toFloat())
//        rect.corner2.shift(x.toFloat(), y.toFloat())
//        rect.corner3.shift(x.toFloat(), y.toFloat())
//        rect.corner4.shift(x.toFloat(), y.toFloat())
//    }

    fun rotate(angleDelta: AngleDegree, focusX: Float, focusY: Float) {
        this.angle.addModulo(angleDelta)
        val angleRad = angleDelta.toRad()
        localRefRotation(angleRad)
        shiftRef(focusX, focusY, angleRad)
    }

    private fun localRefRotation(angle: AngleRad) {
        rect.corner1.rotate(angle)
        rect.corner2.rotate(angle)
        rect.corner3.rotate(angle)
        rect.corner4.rotate(angle)
    }

    private fun shiftRef(focusX: Float, focusY: Float, angle: AngleRad) {
        val shiftX = focusX * cos(angle) - focusY * sin(angle)
        val shiftY = focusX * sin(angle) + focusY * cos(angle)
        rect.corner1.shift(focusX - shiftX, focusY - shiftY)
        rect.corner2.shift(focusX - shiftX, focusY - shiftY)
        rect.corner3.shift(focusX - shiftX, focusY - shiftY)
        rect.corner4.shift(focusX - shiftX, focusY - shiftY)
    }

    private fun Corner.rotate(angle: AngleRad) = apply {
        val newX = x * cos(angle) - y * sin(angle)
        val newY = x * sin(angle) + y * cos(angle)
        x = newX
        y = newY
    }

    private fun Corner.shift(shiftX: Float, shiftY: Float) = apply {
        x += shiftX
        y += shiftY
    }

    fun getRect(): Rectangle = rect
}

package ovh.plrapps.mapview.layout.controller

import ovh.plrapps.mapview.api.MinimumScaleMode
import ovh.plrapps.mapview.layout.controller.GestureController.Controllable
import ovh.plrapps.mapview.util.AngleDegree
import ovh.plrapps.mapview.util.modulo
import ovh.plrapps.mapview.util.scale
import kotlin.math.max
import kotlin.math.min

/**
 * Notifies the [Controllable] with various events, like scale change or referential change.
 *
 * @author P.Laurence on 13/12/19
 */
internal class GestureController(private val controllable: Controllable) {
    private var minScale = Float.MIN_VALUE
    private var maxScale = 1f
    private var effectiveMinScale = 0f
    private var minimumScaleMode = MinimumScaleMode.FIT

    private var shouldLoopScale = true

    internal var isLayoutDone = false
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    private var basePadding: Int = 0
    private var scaledPadding: Int = 0

    internal val scrollLimitX: Int
        get() = userDefinedScrollMaxX?.let {
            scale(it, scale) - screenWidth
        } ?: scaledWidth - screenWidth + scaledPadding

    internal val scrollLimitY: Int
        get() = userDefinedScrollMaxY?.let {
            scale(it, scale) - screenHeight
        } ?: scaledHeight - screenHeight + scaledPadding

    internal val scrollMinX: Int
        get() = userDefinedScrollMinX?.let {
            scale(it, scale)
        } ?: -scaledPadding

    internal val scrollMinY: Int
        get() = userDefinedScrollMinY?.let {
            scale(it, scale)
        } ?: -scaledPadding

    private var userDefinedScrollMinX: Int? = null
    private var userDefinedScrollMinY: Int? = null
    private var userDefinedScrollMaxX: Int? = null
    private var userDefinedScrollMaxY: Int? = null
    private val scrollPosition = ScrollPosition(0, 0)
    private val offsetDestination = OffsetDestination(0, 0, 0f)
    private val viewportCenter = ViewportCenter(0.0, 0.0)

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
            if (this.scale != scaleTmp || !isLayoutDone) {
                val previous = this.scale
                field = scaleTmp
                updateScaledDimensions()
                controllable.constrainScrollToLimits()
                recalculateScaledPadding()
                controllable.onScaleChanged(scaleTmp, previous)
                updateReferential()
                controllable.onContentChanged()
            }
        }

    var rotationEnabled = false
    var handleRotationGesture = true
    var angle: AngleDegree = 0f
        set(value) {
            field = value.modulo()
            updateReferential()
        }

    private fun updateReferential() {
        val (centerX, centerY) = getViewportCenter()
        controllable.onReferentialChanged(angle, scale, centerX, centerY)
    }

    fun notifyScrollChanged() {
        updateReferential()
    }

    fun onRotate(rotationDelta: Float, focusX: Float, focusY: Float) {
        angle += rotationDelta
    }

    /**
     * Beware, this method should only be invoked **after** [setScreenDimensions].
     */
    fun getViewportCenter(): ViewportCenter {
        viewportCenter.x = (scrollPosition.x + min(screenWidth / 2, scaledWidth / 2)).toDouble() / scaledWidth
        viewportCenter.y = (scrollPosition.y + min(screenHeight / 2, scaledHeight / 2)).toDouble() / scaledHeight
        return viewportCenter
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
        controllable.onMinScaleUpdateRequest()
        controllable.constrainScrollToLimits()
        controllable.onLayoutChanged()
    }

    /**
     * Determines whether the [Controllable] should go back to minimum scale after a double-tap at
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
     * @param min Minimum scale the [Controllable] should accept.
     * @param max Maximum scale the [Controllable] should accept.
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
        controllable.onMinScaleUpdateRequest()
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

    fun calculateMinimumScaleToFit(viewportWidth: Int, viewportHeight: Int) {
        val logicalWidth = if (userDefinedScrollMinX != null && userDefinedScrollMaxX != null) {
            userDefinedScrollMaxX!! - userDefinedScrollMinX!!
        } else baseWidth
        val logicalHeight = if (userDefinedScrollMinY != null && userDefinedScrollMaxY != null) {
            userDefinedScrollMaxY!! - userDefinedScrollMinY!!
        } else baseHeight

        val mMinimumScaleX = viewportWidth / logicalWidth.toFloat()
        val mMinimumScaleY = viewportHeight / logicalHeight.toFloat()
        val recalculatedMinScale = calculatedMinScale(mMinimumScaleX, mMinimumScaleY)
        if (recalculatedMinScale != effectiveMinScale) {
            effectiveMinScale = recalculatedMinScale
            if (scale < effectiveMinScale) {
                scale = effectiveMinScale
            }
        }
    }

    fun setConstrainedScroll(x: Int, y: Int): ScrollPosition {
        scrollPosition.x = scrollMinX.coerceAtLeast(min(x, scrollLimitX))
        scrollPosition.y = scrollMinY.coerceAtLeast(min(y, scrollLimitY))
        return scrollPosition
    }

    /**
     * Set the scroll limits at scale 1.0f
     */
    fun setScrollLimits(minX: Int, minY: Int, maxX: Int, maxY: Int) {
        userDefinedScrollMinX = minX
        userDefinedScrollMinY = minY
        userDefinedScrollMaxX = maxX
        userDefinedScrollMaxY = maxY

        /* If the current scroll is outside of the given area, scroll to the nearest position */
        controllable.constrainScrollToLimits()
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

    fun onLayoutDone() {
        isLayoutDone = true
    }

    private fun recalculateScaledPadding() {
        scaledPadding = scale(basePadding, scale)
    }

    interface Controllable {
        fun onMinScaleUpdateRequest()
        fun onLayoutChanged()
        fun onContentChanged()
        fun onScaleChanged(currentScale: Float, previousScale: Float)
        fun onReferentialChanged(angle: AngleDegree, scale: Float, centerX: Double, centerY: Double)
        fun constrainScrollToLimits()
    }

    /**
     * The scroll position is meant to be unique. To avoid creating a lot of objects of this type,
     * we use mutable properties. The usage of this class is meant to be confined to the UI thread.
     */
    data class ScrollPosition(var x: Int, var y: Int)

    data class OffsetDestination(var x: Int, var y: Int, var scale: Float)

    /**
     * The relative coordinates of the center of the view port.
     * This doesn't depend on the scale. For example, if x=0.5 and y=0.3, the center of the viewport
     * is at 50% of [scaledWidth] and at 30% of [scaledHeight].
     */
    data class ViewportCenter(var x: Double, var y: Double)
}

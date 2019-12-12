package com.peterlaurence.mapview.layout

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.core.view.ViewCompat
import com.peterlaurence.mapview.layout.detectors.RotationGestureDetector
import com.peterlaurence.mapview.layout.detectors.TouchUpGestureDetector
import com.peterlaurence.mapview.util.scale
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

/**
 * GestureLayout extends ViewGroup to provide support for scrolling, zooming, and rotating.
 * Fling, drag, pinch and double-tap events are supported natively.
 *
 * Children are laid out to the sizes provided by setSize,
 * and will always be positioned at 0,0 (top-left corner).
 *
 * @author P.Laurence on 12/12/19
 */
open class GestureLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        ViewGroup(context, attrs, defStyleAttr), GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener,
        TouchUpGestureDetector.OnTouchUpListener, RotationGestureDetector.OnRotationGestureListener {

    /**
     * Returns the base (not scaled) width of the underlying composite image.
     *
     * @return The base (not scaled) width of the underlying composite image.
     */
    var baseWidth: Int = 0
        private set
    /**
     * Returns the base (not scaled) height of the underlying composite image.
     *
     * @return The base (not scaled) height of the underlying composite image.
     */
    var baseHeight: Int = 0
        private set
    /**
     * Returns the scaled width of the underlying composite image.
     *
     * @return The scaled width of the underlying composite image.
     */
    var scaledWidth: Int = 0
        private set
    /**
     * Returns the scaled height of the underlying composite image.
     *
     * @return The scaled height of the underlying composite image.
     */
    var scaledHeight: Int = 0
        private set
    private var mImagePadding: Int = 0
    private var mScaledImagePadding: Int = 0

    /**
     * Getter and setter of the scale of the layout.
     */
    var scale = 1f
        set(scale) {
            val scaleTmp = getConstrainedDestinationScale(scale)
            if (this.scale != scaleTmp) {
                val previous = this.scale
                field = scaleTmp
                updateScaledDimensions()
                constrainScrollToLimits()
                recalculateImagePadding()
                onScaleChanged(scaleTmp, previous)
                invalidate()
            }
        }

    private var mMinScale = Float.MIN_VALUE
    private var mMaxScale = 1f

    /**
     * Returns the horizontal distance children are offset if the content is scaled smaller than width.
     *
     * @return
     */
    var offsetX: Int = 0
        private set
    /**
     * Return the vertical distance children are offset if the content is scaled smaller than height.
     *
     * @return
     */
    var offsetY: Int = 0
        private set

    private var mEffectiveMinScale = 0f
    private var mMinimumScaleX: Float = 0.toFloat()
    private var mMinimumScaleY: Float = 0.toFloat()
    private var mShouldLoopScale = true

    /**
     * Returns whether the ZoomPanLayout is currently being flung.
     *
     * @return true if the ZoomPanLayout is currently flinging, false otherwise.
     */
    private var isFlinging: Boolean = false
        private set
    /**
     * Returns whether the ZoomPanLayout is currently being dragged.
     *
     * @return true if the ZoomPanLayout is currently dragging, false otherwise.
     */
    var isDragging: Boolean = false
        private set
    /**
     * Returns whether the ZoomPanLayout is currently operating a scale tween.
     *
     * @return True if the ZoomPanLayout is currently scaling, false otherwise.
     */
    var isScaling: Boolean = false
        private set
    /**
     * Returns whether the ZoomPanLayout is currently operating a scroll tween.
     *
     * @return True if the ZoomPanLayout is currently scrolling, false otherwise.
     */
    var isSliding: Boolean = false
        private set

    /**
     * Returns the duration zoom and pan animations will use.
     *
     * @return The duration zoom and pan animations will use.
     */
    /**
     * Set the duration zoom and pan animation will use.
     *
     * @param animationDuration The duration animations will use.
     */
    var animationDuration = DEFAULT_ZOOM_PAN_ANIMATION_DURATION
        set(duration) {
            field = duration
            animator.duration = duration.toLong()
        }

    private val mScaleGestureDetector: ScaleGestureDetector
    private val mGestureDetector: GestureDetector
    private val mTouchUpGestureDetector: TouchUpGestureDetector
    private val mRotationGestureDetector: RotationGestureDetector by lazy {
        RotationGestureDetector(this)
    }
    private var mMinimumScaleMode = MinimumScaleMode.FIT

    /**
     * Returns the Scroller instance used to manage dragging and flinging.
     *
     * @return The Scroller instance use to manage dragging and flinging.
     */
    // Instantiate default scroller if none is available
    val scroller: Scroller by lazy {
        Scroller(context)
    }

    private val animator: ZoomPanAnimator by lazy {
        val animator = ZoomPanAnimator(this)
        animator.duration = animationDuration.toLong()
        animator
    }

    val halfWidth: Int
        get() = scale(width, 0.5f)

    val halfHeight: Int
        get() = scale(height, 0.5f)

    private val scrollLimitX: Int
        get() = scaledWidth - width + mScaledImagePadding

    private val scrollLimitY: Int
        get() = scaledHeight - height + mScaledImagePadding

    private val scrollMinX: Int
        get() = -mScaledImagePadding

    private val scrollMinY: Int
        get() = -mScaledImagePadding

    init {
        setWillNotDraw(false)
        clipChildren = false
        mGestureDetector = GestureDetector(context, this)
        mScaleGestureDetector = ScaleGestureDetector(context, this)
        mTouchUpGestureDetector = TouchUpGestureDetector(this)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // the container's children should be the size provided by setSize
        // don't use measureChildren because that grabs the child's LayoutParams
        val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(scaledWidth, MeasureSpec.EXACTLY)
        val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(scaledHeight, MeasureSpec.EXACTLY)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec)
        }
        // but the layout itself should report normal (on screen) dimensions
        var width = MeasureSpec.getSize(widthMeasureSpec)
        var height = MeasureSpec.getSize(heightMeasureSpec)
        width = View.resolveSize(width, widthMeasureSpec)
        height = View.resolveSize(height, heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = width
        val height = height

        offsetX = if (scaledWidth >= width) 0 else width / 2 - scaledWidth / 2
        offsetY = if (scaledHeight >= height) 0 else height / 2 - scaledHeight / 2

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                child.layout(offsetX, offsetY, scaledWidth + offsetX, scaledHeight + offsetY)
            }
        }
        calculateMinimumScaleToFit()
        constrainScrollToLimits()
    }

    /**
     * Sets the minimum scale mode
     *
     * @param minimumScaleMode The minimum scale mode
     */
    protected fun setMinimumScaleMode(minimumScaleMode: MinimumScaleMode) {
        mMinimumScaleMode = minimumScaleMode
        calculateMinimumScaleToFit()
    }

    /**
     * Determines whether the ZoomPanLayout should go back to minimum scale after a double-tap at
     * maximum scale.
     *
     * @param shouldLoopScale True to allow going back to minimum scale, false otherwise.
     */
    fun setShouldLoopScale(shouldLoopScale: Boolean) {
        mShouldLoopScale = shouldLoopScale
    }

    /**
     * Set minimum and maximum mScale values for this ZoomPanLayout.
     * Note that if minimumScaleMode is set to [MinimumScaleMode.FIT] or [MinimumScaleMode.FILL], the minimum value set here will be ignored
     * Default values are 0 and 1.
     *
     * @param min Minimum scale the ZoomPanLayout should accept.
     * @param max Maximum scale the ZoomPanLayout should accept.
     */
    fun setScaleLimits(min: Float, max: Float) {
        mMinScale = min
        mMaxScale = max
        scale = scale
    }

    fun setMinScale(min: Float) {
        mMinScale = min
    }

    fun setMaxScale(max: Float) {
        mMaxScale = max
    }

    /**
     * Sets the size (width and height) of the ZoomPanLayout
     * as it should be rendered at a scale of 1f (100%).
     *
     * @param width  Width of the underlying image, not the view or viewport.
     * @param height Height of the underlying image, not the view or viewport.
     */
    open fun setSize(width: Int, height: Int) {
        baseWidth = width
        baseHeight = height
        updateScaledDimensions()
        calculateMinimumScaleToFit()
        constrainScrollToLimits()
        requestLayout()
    }

    /**
     * Adds extra padding around the tiled image, making it possible to scroll past the end of
     * the border even when zoomed in.
     *
     * @param padding  Additional empty padding around the tiled image.
     */
    fun setImagePadding(padding: Int) {
        mImagePadding = padding
        recalculateImagePadding()
    }

    /**
     * Scrolls and centers the ZoomPanLayout to the x and y values provided.
     *
     * @param x Horizontal destination point.
     * @param y Vertical destination point.
     */
    fun scrollToAndCenter(x: Int, y: Int) {
        scrollTo(x - halfWidth, y - halfHeight)
    }

    /**
     * Set the scale of the ZoomPanLayout while maintaining the current center point.
     *
     * @param scale The new value of the ZoomPanLayout scale.
     */
    fun setScaleFromCenter(scale: Float) {
        setScaleFromPosition(halfWidth, halfHeight, scale)
    }

    /**
     * Scrolls the ZoomPanLayout to the x and y values provided using scrolling animation.
     *
     * @param x Horizontal destination point.
     * @param y Vertical destination point.
     */
    fun slideTo(x: Int, y: Int) {
        animator.animatePan(x, y)
    }

    /**
     * Scrolls and centers the ZoomPanLayout to the x and y values provided using scrolling animation.
     *
     * @param x Horizontal destination point.
     * @param y Vertical destination point.
     */
    fun slideToAndCenter(x: Int, y: Int) {
        slideTo(x - halfWidth, y - halfHeight)
    }

    /**
     * Animates the ZoomPanLayout to the scale provided, and centers the viewport to the position
     * supplied.
     *
     * @param x Horizontal destination point.
     * @param y Vertical destination point.
     * @param scale The final scale value the ZoomPanLayout should animate to.
     */
    fun slideToAndCenterWithScale(x: Int, y: Int, scale: Float) {
        animator.animateZoomPan(x - halfWidth, y - halfHeight, scale)
    }

    /**
     * Scales the ZoomPanLayout with animated progress, without maintaining scroll position.
     *
     * @param destination The final scale value the ZoomPanLayout should animate to.
     */
    fun smoothScaleTo(destination: Float) {
        animator.animateZoom(destination)
    }

    /**
     * Animates the ZoomPanLayout to the scale provided, while maintaining position determined by
     * the focal point provided.
     *
     * @param focusX The horizontal focal point to maintain, relative to the screen (as supplied by MotionEvent.getX).
     * @param focusY The vertical focal point to maintain, relative to the screen (as supplied by MotionEvent.getY).
     * @param scale The final scale value the ZoomPanLayout should animate to.
     */
    fun smoothScaleFromFocalPoint(focusX: Int, focusY: Int, scale: Float) {
        var scale = scale
        scale = getConstrainedDestinationScale(scale)
        if (scale == this.scale) {
            return
        }
        val x = getOffsetScrollXFromScale(focusX, scale, this.scale)
        val y = getOffsetScrollYFromScale(focusY, scale, this.scale)
        animator.animateZoomPan(x, y, scale)
    }

    /**
     * Animate the scale of the ZoomPanLayout while maintaining the current center point.
     *
     * @param scale The final scale value the ZoomPanLayout should animate to.
     */
    fun smoothScaleFromCenter(scale: Float) {
        smoothScaleFromFocalPoint(halfWidth, halfHeight, scale)
    }

    /**
     * Provide this method to be overriden by subclasses, e.g., onScrollChanged.
     */
    open fun onScaleChanged(currentScale: Float, previousScale: Float) {
        // noop
    }

    private fun getConstrainedDestinationScale(scale: Float): Float {
        var scaleTmp = scale
        scaleTmp = max(scaleTmp, mEffectiveMinScale)
        scaleTmp = min(scaleTmp, mMaxScale)
        return scaleTmp
    }

    private fun constrainScrollToLimits() {
        val x = scrollX
        val y = scrollY
        val constrainedX = getConstrainedScrollX(x)
        val constrainedY = getConstrainedScrollY(y)
        if (x != constrainedX || y != constrainedY) {
            scrollTo(constrainedX, constrainedY)
        }
    }

    private fun updateScaledDimensions() {
        scaledWidth = scale(baseWidth, scale)
        scaledHeight = scale(baseHeight, scale)
    }

    private fun getOffsetScrollXFromScale(offsetX: Int, destinationScale: Float, currentScale: Float): Int {
        val scrollX = scrollX + offsetX
        val deltaScale = destinationScale / currentScale
        return (scrollX * deltaScale).toInt() - offsetX
    }

    private fun getOffsetScrollYFromScale(offsetY: Int, destinationScale: Float, currentScale: Float): Int {
        val scrollY = scrollY + offsetY
        val deltaScale = destinationScale / currentScale
        return (scrollY * deltaScale).toInt() - offsetY
    }

    fun setScaleFromPosition(offsetX: Int, offsetY: Int, scale: Float) {
        val scaleCst = getConstrainedDestinationScale(scale)
        if (scaleCst == this.scale) {
            return
        }
        var x = getOffsetScrollXFromScale(offsetX, scaleCst, this.scale)
        var y = getOffsetScrollYFromScale(offsetY, scaleCst, this.scale)

        this.scale = scaleCst

        x = getConstrainedScrollX(x)
        y = getConstrainedScrollY(y)

        scrollTo(x, y)
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        val position = scrollX
        return if (direction > 0) position < scrollLimitX else direction < 0 && position > 0
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val gestureIntercept = mGestureDetector.onTouchEvent(event)
        val scaleIntercept = mScaleGestureDetector.onTouchEvent(event)
        val touchIntercept = mTouchUpGestureDetector.onTouchEvent(event)
        val rotationIntercept = mRotationGestureDetector.onTouchEvent(event)
        return gestureIntercept || scaleIntercept || touchIntercept || super.onTouchEvent(event) || rotationIntercept
    }

    override fun scrollTo(x: Int, y: Int) {
        super.scrollTo(getConstrainedScrollX(x), getConstrainedScrollY(y))
    }

    private fun calculateMinimumScaleToFit() {
        mMinimumScaleX = width / baseWidth.toFloat()
        mMinimumScaleY = height / baseHeight.toFloat()
        val recalculatedMinScale = calculatedMinScale(mMinimumScaleX, mMinimumScaleY)
        if (recalculatedMinScale != mEffectiveMinScale) {
            mEffectiveMinScale = recalculatedMinScale
            if (scale < mEffectiveMinScale) {
                scale = mEffectiveMinScale
            }
        }
    }

    private fun calculatedMinScale(minimumScaleX: Float, minimumScaleY: Float): Float {
        return when (mMinimumScaleMode) {
            MinimumScaleMode.FILL -> max(minimumScaleX, minimumScaleY)
            MinimumScaleMode.FIT -> min(minimumScaleX, minimumScaleY)
            MinimumScaleMode.NONE -> mMinScale
        }
    }

    /**
     * When the scale is less than `mMinimumScaleX`, either because we are using
     * [MinimumScaleMode.FIT] or [MinimumScaleMode.NONE], the scroll position takes a
     * value between its starting value and 0. A linear interpolation between the
     * `mMinimumScaleX` and the `mEffectiveMinScale` is used.
     *
     *
     * This strategy is used to avoid that a custom return value of [.getScrollMinX] (which
     * default to 0) become the return value of this method which shifts the whole layout.
     */
    protected fun getConstrainedScrollX(x: Int): Int {
        // TODO: is this if condition really useful?
        if (scale < mMinimumScaleX && mEffectiveMinScale != mMinimumScaleX) {
            val scaleFactor = scale / (mMinimumScaleX - mEffectiveMinScale) + mEffectiveMinScale / (mEffectiveMinScale - mMinimumScaleX)
            return (scaleFactor * scrollX).toInt()
        }
        return Math.max(scrollMinX, Math.min(x, scrollLimitX))
    }

    /**
     * See [.getConstrainedScrollX]
     */
    protected fun getConstrainedScrollY(y: Int): Int {
        // TODO: is this if condition really useful?
        if (scale < mMinimumScaleY && mEffectiveMinScale != mMinimumScaleY) {
            val scaleFactor = scale / (mMinimumScaleY - mEffectiveMinScale) + mEffectiveMinScale / (mEffectiveMinScale - mMinimumScaleY)
            return (scaleFactor * scrollY).toInt()
        }
        return Math.max(scrollMinY, Math.min(y, scrollLimitY))
    }

    private fun recalculateImagePadding() {
        mScaledImagePadding = scale(mImagePadding, scale)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val startX = scrollX
            val startY = scrollY
            val endX = getConstrainedScrollX(scroller.currX)
            val endY = getConstrainedScrollY(scroller.currY)
            if (startX != endX || startY != endY) {
                scrollTo(endX, endY)
            }
            if (scroller.isFinished) {
                if (isFlinging) {
                    isFlinging = false
                }
            } else {
                ViewCompat.postInvalidateOnAnimation(this)
            }
        }
    }

    override fun onDown(event: MotionEvent): Boolean {
        if (isFlinging && !scroller.isFinished) {
            scroller.forceFinished(true)
            isFlinging = false
        }
        return true
    }

    override fun onFling(event1: MotionEvent, event2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        scroller.fling(scrollX, scrollY, (-velocityX).toInt(), (-velocityY).toInt(),
                scrollMinX, scrollLimitX, scrollMinY, scrollLimitY)

        isFlinging = true
        ViewCompat.postInvalidateOnAnimation(this)
        return true
    }

    override fun onLongPress(event: MotionEvent) {

    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        val scrollEndX = scrollX + distanceX.toInt()
        val scrollEndY = scrollY + distanceY.toInt()
        scrollTo(scrollEndX, scrollEndY)
        if (!isDragging) {
            isDragging = true
        }
        return true
    }

    override fun onShowPress(event: MotionEvent) {

    }

    override fun onSingleTapUp(event: MotionEvent): Boolean {
        return true
    }

    override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
        return true
    }

    override fun onDoubleTap(event: MotionEvent): Boolean {
        var destination = Math.pow(2.0, Math.floor(Math.log((scale * 2).toDouble()) / Math.log(2.0))).toFloat()
        val effectiveDestination = if (mShouldLoopScale && scale >= mMaxScale) mMinScale else destination
        destination = getConstrainedDestinationScale(effectiveDestination)
        smoothScaleFromFocalPoint(event.x.toInt(), event.y.toInt(), destination)
        return true
    }

    override fun onDoubleTapEvent(event: MotionEvent): Boolean {
        return true
    }

    override fun onTouchUp(event: MotionEvent): Boolean {
        if (isDragging) {
            isDragging = false
        }
        return true
    }

    override fun onScaleBegin(scaleGestureDetector: ScaleGestureDetector): Boolean {
        isScaling = true
        return true
    }

    override fun onScaleEnd(scaleGestureDetector: ScaleGestureDetector) {
        isScaling = false
    }

    override fun onScale(scaleGestureDetector: ScaleGestureDetector): Boolean {
        val currentScale = scale * mScaleGestureDetector.scaleFactor
        setScaleFromPosition(
                scaleGestureDetector.focusX.toInt(),
                scaleGestureDetector.focusY.toInt(),
                currentScale)
        return true
    }

    override fun onRotate(rotationDelta: Float, focusX: Float, focusY: Float): Boolean {
        println("rotate $rotationDelta ($focusX ; $focusY)")
        return true
    }

    override fun onRotationBegin(): Boolean {
        println("rotate start")
        return true
    }

    override fun onRotationEnd() {
        println("rotate end")
    }

    private class ZoomPanAnimator(gestureLayout: GestureLayout) : ValueAnimator(), ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

        private val mGestureLayoutWeakReference: WeakReference<GestureLayout>
        private val mStartState = ZoomPanState()
        private val mEndState = ZoomPanState()
        private var mHasPendingZoomUpdates: Boolean = false
        private var mHasPendingPanUpdates: Boolean = false

        init {
            addUpdateListener(this)
            addListener(this)
            setFloatValues(0f, 1f)
            interpolator = FastEaseInInterpolator()
            mGestureLayoutWeakReference = WeakReference(gestureLayout)
        }

        private fun setupPanAnimation(x: Int, y: Int): Boolean {
            val zoomPanLayout = mGestureLayoutWeakReference.get()
            if (zoomPanLayout != null) {
                mStartState.x = zoomPanLayout.scrollX
                mStartState.y = zoomPanLayout.scrollY
                mEndState.x = x
                mEndState.y = y
                return mStartState.x != mEndState.x || mStartState.y != mEndState.y
            }
            return false
        }

        private fun setupZoomAnimation(scale: Float): Boolean {
            val zoomPanLayout = mGestureLayoutWeakReference.get()
            if (zoomPanLayout != null) {
                mStartState.scale = zoomPanLayout.scale
                mEndState.scale = scale
                return mStartState.scale != mEndState.scale
            }
            return false
        }

        fun animateZoomPan(x: Int, y: Int, scale: Float) {
            val zoomPanLayout = mGestureLayoutWeakReference.get()
            if (zoomPanLayout != null) {
                mHasPendingZoomUpdates = setupZoomAnimation(scale)
                mHasPendingPanUpdates = setupPanAnimation(x, y)
                if (mHasPendingPanUpdates || mHasPendingZoomUpdates) {
                    start()
                }
            }
        }

        fun animateZoom(scale: Float) {
            val zoomPanLayout = mGestureLayoutWeakReference.get()
            if (zoomPanLayout != null) {
                mHasPendingZoomUpdates = setupZoomAnimation(scale)
                if (mHasPendingZoomUpdates) {
                    start()
                }
            }
        }

        fun animatePan(x: Int, y: Int) {
            val zoomPanLayout = mGestureLayoutWeakReference.get()
            if (zoomPanLayout != null) {
                mHasPendingPanUpdates = setupPanAnimation(x, y)
                if (mHasPendingPanUpdates) {
                    start()
                }
            }
        }

        override fun onAnimationUpdate(animation: ValueAnimator) {
            val zoomPanLayout = mGestureLayoutWeakReference.get()
            if (zoomPanLayout != null) {
                val progress = animation.animatedValue as Float
                if (mHasPendingZoomUpdates) {
                    val scale = mStartState.scale + (mEndState.scale - mStartState.scale) * progress
                    zoomPanLayout.scale = scale
                }
                if (mHasPendingPanUpdates) {
                    val x = (mStartState.x + (mEndState.x - mStartState.x) * progress).toInt()
                    val y = (mStartState.y + (mEndState.y - mStartState.y) * progress).toInt()
                    zoomPanLayout.scrollTo(x, y)
                }
            }
        }

        override fun onAnimationStart(animator: Animator) {
            val zoomPanLayout = mGestureLayoutWeakReference.get()
            if (zoomPanLayout != null) {
                if (mHasPendingZoomUpdates) {
                    zoomPanLayout.isScaling = true
                }
                if (mHasPendingPanUpdates) {
                    zoomPanLayout.isSliding = true
                }
            }
        }

        override fun onAnimationEnd(animator: Animator) {
            val zoomPanLayout = mGestureLayoutWeakReference.get()
            if (zoomPanLayout != null) {
                if (mHasPendingZoomUpdates) {
                    mHasPendingZoomUpdates = false
                    zoomPanLayout.isScaling = false
                }
                if (mHasPendingPanUpdates) {
                    mHasPendingPanUpdates = false
                    zoomPanLayout.isSliding = false
                }
            }
        }

        override fun onAnimationCancel(animator: Animator) {
            onAnimationEnd(animator)
        }

        override fun onAnimationRepeat(animator: Animator) {

        }

        private class ZoomPanState {
            var x: Int = 0
            var y: Int = 0
            var scale: Float = 0.toFloat()
        }

        private class FastEaseInInterpolator : Interpolator {
            override fun getInterpolation(input: Float): Float {
                return (1 - Math.pow((1 - input).toDouble(), 8.0)).toFloat()
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

    companion object {
        private const val DEFAULT_ZOOM_PAN_ANIMATION_DURATION = 400
    }
}


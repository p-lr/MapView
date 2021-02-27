package ovh.plrapps.mapview.layout

import android.content.Context
import android.util.AttributeSet
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.core.view.ViewCompat
import ovh.plrapps.mapview.layout.animators.ZoomPanAnimator
import ovh.plrapps.mapview.layout.controller.GestureController
import ovh.plrapps.mapview.layout.detectors.RotationGestureDetector
import ovh.plrapps.mapview.layout.detectors.TouchUpGestureDetector
import ovh.plrapps.mapview.util.scale
import ovh.plrapps.mapview.util.toRad
import kotlin.math.*

/**
 * GestureLayout provides support for scrolling, zooming, and rotating.
 * Fling, drag, pinch and double-tap events are supported natively.
 *
 * @author P.Laurence on 12/12/19
 */
abstract class GestureLayout @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
        ViewGroup(context, attrs, defStyleAttr), GestureDetector.OnGestureListener,
        GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener,
        TouchUpGestureDetector.OnTouchUpListener, RotationGestureDetector.OnRotationGestureListener,
        GestureController.Controllable {

    internal val gestureController: GestureController by lazy { GestureController(this) }

    private val defaultInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    private val fastInterpolator: Interpolator = DecelerateInterpolator(2f)

    override fun onMinScaleUpdateRequest() {
        gestureController.calculateMinimumScaleToFit(width, height)
    }

    /**
     * Handle on the scale property.
     * If the view isn't laid-out by the time this property is set, the scale will be set on the
     * next layout pass.
     */
    var scale: Float
        get() = gestureController.scale
        set(value) {
            if (isLaidOut) {
                gestureController.scale = value
            } else {
                post {
                    gestureController.scale = value
                }
            }
        }

    /**
     * The horizontal distance children are offset if the content is scaled smaller than width.
     */
    var offsetX: Int = 0
        private set

    /**
     * The vertical distance children are offset if the content is scaled smaller than height.
     */
    var offsetY: Int = 0
        private set

    /**
     * Whether the [GestureLayout] is currently being flung.
     */
    var isFlinging: Boolean = false
        private set

    /**
     * Whether the [GestureLayout] is currently being dragged.
     */
    var isDragging: Boolean = false
        private set

    /**
     * Whether the [GestureLayout] is currently scaling.
     */
    var isScaling: Boolean = false
        private set

    /**
     * Whether the [GestureLayout] is currently currently scrolling.
     */
    var isSliding: Boolean = false
        private set

    /**
     * Set the duration zoom and pan animation will use.
     */
    var animationDuration = DEFAULT_ZOOM_PAN_ANIMATION_DURATION
        set(duration) {
            field = duration
            animator.duration = duration.toLong()
        }

    private val scaleGestureDetector: ScaleGestureDetector by lazy {
        ScaleGestureDetector(context, this)
    }
    private val gestureDetector: GestureDetector by lazy {
        GestureDetector(context, this)
    }
    private val touchUpGestureDetector: TouchUpGestureDetector by lazy {
        TouchUpGestureDetector(this)
    }
    private val rotationGestureDetector: RotationGestureDetector by lazy {
        RotationGestureDetector(this)
    }

    /* The Scroller instance used to manage dragging and flinging */
    private val scroller: Scroller by lazy {
        Scroller(context)
    }

    private val animator: ZoomPanAnimator by lazy {
        val animator = ZoomPanAnimator(object : ZoomPanAnimator.OnZoomPanAnimationListener {
            override fun setIsScaling(isScaling: Boolean) {
                this@GestureLayout.isScaling = isScaling
            }

            override fun setIsSliding(isSliding: Boolean) {
                this@GestureLayout.isSliding = isSliding
            }

            override fun setScale(scale: Float) {
                this@GestureLayout.gestureController.scale = scale
            }

            override fun scrollTo(x: Int, y: Int) {
                this@GestureLayout.scrollTo(x, y)
            }

            override fun getScrollX(): Int = this@GestureLayout.scrollX
            override fun getScrollY(): Int = this@GestureLayout.scrollY
            override fun getScale(): Float = this@GestureLayout.gestureController.scale

        })
        animator.duration = animationDuration.toLong()
        animator
    }

    val halfWidth: Int
        get() = scale(width, 0.5f)

    val halfHeight: Int
        get() = scale(height, 0.5f)


    init {
        clipChildren = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // the container's children should be the size provided by setSize
        // don't use measureChildren because that grabs the child's LayoutParams
        val childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(gestureController.scaledWidth, MeasureSpec.EXACTLY)
        val childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(gestureController.scaledHeight, MeasureSpec.EXACTLY)
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
        val width = width       // width of screen in pixels
        val height = height     // height on screen in pixels

        gestureController.setScreenDimensions(width, height)

        val scaledWidth = gestureController.scaledWidth
        val scaledHeight = gestureController.scaledHeight

        offsetX = if (scaledWidth >= width) 0 else width / 2 - scaledWidth / 2
        offsetY = if (scaledHeight >= height) 0 else height / 2 - scaledHeight / 2

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                child.layout(offsetX, offsetY, scaledWidth + offsetX, scaledHeight + offsetY)
            }
        }
        onMinScaleUpdateRequest()
        constrainScrollToLimits()

        gestureController.onLayoutDone()
    }

    override fun onLayoutChanged() {
        requestLayout()
    }

    override fun onContentChanged() {
        invalidate()
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)

        gestureController.notifyScrollChanged()
    }

    /**
     * Scrolls and centers the [GestureLayout] to the x and y values provided.
     *
     * @param x Horizontal destination point.
     * @param y Vertical destination point.
     */
    fun scrollToAndCenter(x: Int, y: Int) {
        scrollTo(x - halfWidth, y - halfHeight)
    }

    /**
     * Set the scale of the [GestureLayout] while maintaining the current center point.
     */
    fun setScaleFromCenter(scale: Float) {
        setScaleFromPosition(halfWidth, halfHeight, scale)
    }

    /**
     * Scrolls the [GestureLayout] to the x and y values provided using scrolling animation.
     *
     * @param x Horizontal destination point.
     * @param y Vertical destination point.
     * @param interpolator The [Interpolator] the animation should use.
     */
    fun slideTo(x: Int, y: Int, interpolator: Interpolator = defaultInterpolator) {
        animator.animatePan(x, y, interpolator)
    }

    /**
     * Scrolls and centers the [GestureLayout] to the x and y values provided using scrolling animation.
     *
     * @param x Horizontal destination point.
     * @param y Vertical destination point.
     * @param interpolator The [Interpolator] the animation should use.
     */
    @Suppress("unused")
    fun slideToAndCenter(x: Int, y: Int, interpolator: Interpolator = defaultInterpolator) {
        slideTo(x - halfWidth, y - halfHeight, interpolator)
    }

    /**
     * Animates the [GestureLayout] to the scale provided, and centers the viewport to the position
     * supplied.
     *
     * @param x Horizontal destination point.
     * @param y Vertical destination point.
     * @param scale The final scale value the layout should animate to.
     * @param interpolator The [Interpolator] the animation should use.
     */
    fun slideToAndCenterWithScale(x: Int, y: Int, scale: Float, interpolator: Interpolator = defaultInterpolator) {
        animator.animateZoomPan(x - halfWidth, y - halfHeight, scale, interpolator)
    }

    /**
     * Scales the [GestureLayout] with animated progress, without maintaining scroll position.
     *
     * @param destination The final scale value the layout should animate to.
     * @param interpolator The [Interpolator] the animation should use.
     */
    @Suppress("unused")
    fun smoothScaleTo(destination: Float, interpolator: Interpolator = defaultInterpolator) {
        animator.animateZoom(destination, interpolator)
    }

    /**
     * Animates the [GestureLayout] to the scale provided, while maintaining position determined by
     * the focal point provided.
     *
     * @param focusX The horizontal focal point to maintain, relative to the screen (as supplied by MotionEvent.getX).
     * @param focusY The vertical focal point to maintain, relative to the screen (as supplied by MotionEvent.getY).
     * @param scale The final scale value the layout should animate to.
     * @param interpolator The [Interpolator] the animation should use.
     */
    fun smoothScaleFromFocalPoint(focusX: Int, focusY: Int, scale: Float, interpolator: Interpolator = defaultInterpolator) {
        val (x, y, scaleCst) = gestureController.getOffsetDestination(focusX, focusY, scale)
        if (scaleCst == gestureController.scale) {
            return
        }
        animator.animateZoomPan(x, y, scaleCst, interpolator)
    }

    /**
     * Animate the scale of the [GestureLayout] while maintaining the current center point.
     *
     * @param scale The final scale value the layout should animate to.
     * @param interpolator The [Interpolator] the animation should use.
     */
    @Suppress("unused")
    fun smoothScaleFromCenter(scale: Float, interpolator: Interpolator = defaultInterpolator) {
        smoothScaleFromFocalPoint(halfWidth, halfHeight, scale, interpolator)
    }

    override fun constrainScrollToLimits() {
        val x = scrollX
        val y = scrollY
        val (constrainedX, constrainedY) = gestureController.setConstrainedScroll(x, y)
        if (x != constrainedX || y != constrainedY) {
            super.scrollTo(constrainedX, constrainedY)
        }
    }

    private fun setScaleFromPosition(offsetX: Int, offsetY: Int, scale: Float) {
        val (x, y, scaleCst) = gestureController.getOffsetDestination(offsetX, offsetY, scale)
        if (scaleCst == gestureController.scale) {
            return
        }

        this.gestureController.scale = scaleCst

        scrollTo(x, y)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val gestureIntercept = gestureDetector.onTouchEvent(event)
        val scaleIntercept = scaleGestureDetector.onTouchEvent(event)
        val touchIntercept = touchUpGestureDetector.onTouchEvent(event)
        val rotationIntercept = if (gestureController.rotationEnabled && gestureController.handleRotationGesture) {
            rotationGestureDetector.onTouchEvent(event)
        } else false
        return gestureIntercept || scaleIntercept || touchIntercept || super.onTouchEvent(event) || rotationIntercept
    }

    /**
     * Set the scroll location in pixels.
     * If the view isn't laid-out by the time this method is invoked, the scroll location will be
     * set on the next layout pass.
     */
    override fun scrollTo(x: Int, y: Int) {
        val scrollAction = {
            val (constrainedX, constrainedY) = gestureController.setConstrainedScroll(x, y)
            super.scrollTo(constrainedX, constrainedY)
        }
        if (isLaidOut) {
            scrollAction()
        } else {
            post {
                scrollAction()
            }
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            val startX = scrollX
            val startY = scrollY
            val (endX, endY) = gestureController.setConstrainedScroll(scroller.currX, scroller.currY)
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
        val c = gestureController
        if (gestureController.angle == 0f) { // fast path
            scroller.fling(scrollX, scrollY, (-velocityX).toInt(), (-velocityY).toInt(),
                    c.scrollMinX, c.scrollLimitX, c.scrollMinY, c.scrollLimitY)
        } else {
            val angleRad = -gestureController.angle.toRad()
            val velocityXr = velocityX * cos(angleRad) - velocityY * sin(angleRad)
            val velocityYr = velocityX * sin(angleRad) + velocityY * cos(angleRad)
            scroller.fling(scrollX, scrollY, (-velocityXr).toInt(), (-velocityYr).toInt(),
                    c.scrollMinX, c.scrollLimitX, c.scrollMinY, c.scrollLimitY)
        }

        isFlinging = true
        ViewCompat.postInvalidateOnAnimation(this)
        return true
    }

    override fun onLongPress(event: MotionEvent) {

    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (gestureController.angle == 0f) {  // fast path
            val scrollEndX = scrollX + distanceX.toInt()
            val scrollEndY = scrollY + distanceY.toInt()
            scrollTo(scrollEndX, scrollEndY)
        } else {
            val angleRad = -gestureController.angle.toRad()
            val distanceXr = distanceX * cos(angleRad) - distanceY * sin(angleRad)
            val distanceYr = distanceX * sin(angleRad) + distanceY * cos(angleRad)
            val scrollEndX = scrollX + distanceXr.toInt()
            val scrollEndY = scrollY + distanceYr.toInt()
            scrollTo(scrollEndX, scrollEndY)
        }

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
        val destination = 2.0.pow(floor(
                ln((gestureController.scale * 2).toDouble()) / ln(2.0))).toFloat()
        val scaleCst = gestureController.getDoubleTapDestinationScale(destination,
                gestureController.scale)

        if (gestureController.angle == 0f) {
            smoothScaleFromFocalPoint(event.x.toInt(), event.y.toInt(), scaleCst, fastInterpolator)
        } else {
            val angleRad = -gestureController.angle.toRad()
            val eventRx = (height / 2 * sin(angleRad) + width / 2 * (1 - cos(angleRad)) +
                    event.x * cos(angleRad) - event.y * sin(angleRad)).toInt()
            val eventRy = (height / 2 * (1 - cos(angleRad)) - width / 2 * sin(angleRad) +
                    event.x * sin(angleRad) + event.y * cos(angleRad)).toInt()
            smoothScaleFromFocalPoint(eventRx, eventRy, scaleCst, fastInterpolator)
        }

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
        val newScale = gestureController.scale * this.scaleGestureDetector.scaleFactor
        setScaleFromPosition(
                scaleGestureDetector.focusX.toInt(),
                scaleGestureDetector.focusY.toInt(),
                newScale)
        return true
    }

    override fun onRotate(rotationDelta: Float, focusX: Float, focusY: Float): Boolean {
        gestureController.onRotate(rotationDelta, focusX, focusY)
        return true
    }

    override fun onRotationBegin(): Boolean {
        return true
    }

    override fun onRotationEnd() {
    }

    companion object {
        private const val DEFAULT_ZOOM_PAN_ANIMATION_DURATION = 400
    }
}

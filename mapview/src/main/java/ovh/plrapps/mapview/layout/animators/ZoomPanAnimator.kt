package ovh.plrapps.mapview.layout.animators

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.animation.Interpolator

class ZoomPanAnimator(private val listener: OnZoomPanAnimationListener) : ValueAnimator(),
    ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    private val startState = ZoomPanState()
    private val endState = ZoomPanState()
    private var hasPendingZoomUpdates: Boolean = false
    private var hasPendingPanUpdates: Boolean = false

    init {
        addUpdateListener(this)
        addListener(this)
        setFloatValues(0f, 1f)
    }

    private fun setupPanAnimation(x: Int, y: Int): Boolean {
        startState.x = listener.getScrollX()
        startState.y = listener.getScrollY()
        endState.x = x
        endState.y = y
        return startState.x != endState.x || startState.y != endState.y
    }

    private fun setupZoomAnimation(scale: Float): Boolean {
        startState.scale = listener.getScale()
        endState.scale = scale
        return startState.scale != endState.scale
    }

    fun animateZoomPan(x: Int, y: Int, scale: Float, interpolator: Interpolator) {
        hasPendingZoomUpdates = setupZoomAnimation(scale)
        hasPendingPanUpdates = setupPanAnimation(x, y)
        if (hasPendingPanUpdates || hasPendingZoomUpdates) {
            this.interpolator = interpolator
            start()
        }
    }

    fun animateZoom(scale: Float, interpolator: Interpolator) {
        hasPendingZoomUpdates = setupZoomAnimation(scale)
        if (hasPendingZoomUpdates) {
            this.interpolator = interpolator
            start()
        }
    }

    fun animatePan(x: Int, y: Int, interpolator: Interpolator) {
        hasPendingPanUpdates = setupPanAnimation(x, y)
        if (hasPendingPanUpdates) {
            this.interpolator = interpolator
            start()
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        val progress = animation.animatedValue as Float
        if (hasPendingZoomUpdates) {
            val scale = startState.scale + (endState.scale - startState.scale) * progress
            listener.setScale(scale)
        }
        if (hasPendingPanUpdates) {
            val x = (startState.x + (endState.x - startState.x) * progress).toInt()
            val y = (startState.y + (endState.y - startState.y) * progress).toInt()
            listener.scrollTo(x, y)
        }
    }

    override fun onAnimationStart(animator: Animator) {
        if (hasPendingZoomUpdates) {
            listener.setIsScaling(true)
        }
        if (hasPendingPanUpdates) {
            listener.setIsSliding(true)
        }
    }

    override fun onAnimationEnd(animator: Animator) {
        if (hasPendingZoomUpdates) {
            hasPendingZoomUpdates = false
            listener.setIsScaling(false)
        }
        if (hasPendingPanUpdates) {
            hasPendingPanUpdates = false
            listener.setIsSliding(false)
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

    interface OnZoomPanAnimationListener {
        fun setIsScaling(isScaling: Boolean)
        fun setIsSliding(isSliding: Boolean)
        fun setScale(scale: Float)
        fun scrollTo(x: Int, y: Int)
        fun getScrollX(): Int
        fun getScrollY(): Int
        fun getScale(): Float
    }
}
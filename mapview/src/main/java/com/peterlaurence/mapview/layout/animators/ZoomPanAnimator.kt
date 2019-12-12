package com.peterlaurence.mapview.layout.animators

import android.animation.Animator
import android.animation.ValueAnimator
import android.view.animation.Interpolator
import kotlin.math.pow

class ZoomPanAnimator(private val listener: OnZoomPanAnimationListener) : ValueAnimator(),
    ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {

    private val mStartState = ZoomPanState()
    private val mEndState = ZoomPanState()
    private var mHasPendingZoomUpdates: Boolean = false
    private var mHasPendingPanUpdates: Boolean = false

    init {
        addUpdateListener(this)
        addListener(this)
        setFloatValues(0f, 1f)
        interpolator = FastEaseInInterpolator()
    }

    private fun setupPanAnimation(x: Int, y: Int): Boolean {
        mStartState.x = listener.getScrollX()
        mStartState.y = listener.getScrollY()
        mEndState.x = x
        mEndState.y = y
        return mStartState.x != mEndState.x || mStartState.y != mEndState.y
    }

    private fun setupZoomAnimation(scale: Float): Boolean {
        mStartState.scale = listener.getScale()
        mEndState.scale = scale
        return mStartState.scale != mEndState.scale
    }

    fun animateZoomPan(x: Int, y: Int, scale: Float) {
        mHasPendingZoomUpdates = setupZoomAnimation(scale)
        mHasPendingPanUpdates = setupPanAnimation(x, y)
        if (mHasPendingPanUpdates || mHasPendingZoomUpdates) {
            start()
        }
    }

    fun animateZoom(scale: Float) {
        mHasPendingZoomUpdates = setupZoomAnimation(scale)
        if (mHasPendingZoomUpdates) {
            start()
        }
    }

    fun animatePan(x: Int, y: Int) {
        mHasPendingPanUpdates = setupPanAnimation(x, y)
        if (mHasPendingPanUpdates) {
            start()
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        val progress = animation.animatedValue as Float
        if (mHasPendingZoomUpdates) {
            val scale = mStartState.scale + (mEndState.scale - mStartState.scale) * progress
            listener.setScale(scale)
        }
        if (mHasPendingPanUpdates) {
            val x = (mStartState.x + (mEndState.x - mStartState.x) * progress).toInt()
            val y = (mStartState.y + (mEndState.y - mStartState.y) * progress).toInt()
            listener.scrollTo(x, y)
        }
    }

    override fun onAnimationStart(animator: Animator) {
        if (mHasPendingZoomUpdates) {
            listener.setIsScaling(true)
        }
        if (mHasPendingPanUpdates) {
            listener.setIsSliding(true)
        }
    }

    override fun onAnimationEnd(animator: Animator) {
        if (mHasPendingZoomUpdates) {
            mHasPendingZoomUpdates = false
            listener.setIsScaling(false)
        }
        if (mHasPendingPanUpdates) {
            mHasPendingPanUpdates = false
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

    private class FastEaseInInterpolator : Interpolator {
        override fun getInterpolation(input: Float): Float {
            return (1 - (1 - input).toDouble().pow(8.0)).toFloat()
        }
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
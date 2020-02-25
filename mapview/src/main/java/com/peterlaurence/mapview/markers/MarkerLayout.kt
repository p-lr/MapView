package com.peterlaurence.mapview.markers

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.ReferentialOwner
import com.peterlaurence.mapview.ReferentialData
import com.peterlaurence.mapview.util.rotateCenteredX
import com.peterlaurence.mapview.util.rotateCenteredY
import com.peterlaurence.mapview.util.toRad

/**
 * All markers are laid out using this view.
 * All apis related to makers are implemented as extension functions on the [MapView], like for
 * example [MapView.addMarker].
 *
 * @author peterLaurence on 18/06/2019
 */
open class MarkerLayout(context: Context) : ViewGroup(context), ReferentialOwner {

    override var referentialData = ReferentialData(false)
        set(value) {
            field = value
            requestLayout()
            refreshPositions()
        }
    private var markerTapListener: MarkerTapListener? = null
    private val calloutViewList = mutableListOf<View>()

    init {
        clipChildren = false
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(availableWidth, availableHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                populateLayoutParams(child)
                val layoutParams = child.layoutParams as MarkerLayoutParams
                child.layout(layoutParams.left, layoutParams.top, layoutParams.right, layoutParams.bottom)
            }
        }
    }

    private fun populateLayoutParams(child: View): MarkerLayoutParams {
        val layoutParams = child.layoutParams as MarkerLayoutParams
        if (child.visibility != View.GONE) {
            // actual sizes of children
            val actualWidth = child.measuredWidth
            val actualHeight = child.measuredHeight
            // calculate combined anchor offsets
            val widthOffset = actualWidth * layoutParams.relativeAnchorX + layoutParams.absoluteAnchorX
            val heightOffset = actualHeight * layoutParams.relativeAnchorY + layoutParams.absoluteAnchorY
            // get offset position
            val scaledX = layoutParams.x * referentialData.scale
            val scaledY = layoutParams.y * referentialData.scale

            if (referentialData.rotationEnabled) {
                val centerX = referentialData.centerX * measuredWidth
                val centerY = referentialData.centerY * measuredHeight

                val angleRad = referentialData.angle.toRad()
                layoutParams.left = (rotateCenteredX(scaledX, scaledY, centerX, centerY, angleRad) + widthOffset).toInt()
                layoutParams.top = (rotateCenteredY(scaledX, scaledY, centerX, centerY, angleRad) + heightOffset).toInt()
                layoutParams.right = layoutParams.left + actualWidth
                layoutParams.bottom = layoutParams.top + actualHeight

                if (layoutParams.shouldRotateWithMap) {
                    // rotates Marker with map by current map degrees or keeps fixedAngle
                    child.rotation = layoutParams.fixedAngle?.plus(referentialData.angle) ?: referentialData.angle
                }
            } else {
                // save computed values
                layoutParams.left = (scaledX + widthOffset).toInt()
                layoutParams.top = (scaledY + heightOffset).toInt()
                layoutParams.right = layoutParams.left + actualWidth
                layoutParams.bottom = layoutParams.top + actualHeight
            }
        }
        return layoutParams
    }

    private fun refreshPositions() {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val layoutParams: MarkerLayoutParams = populateLayoutParams(child)
                child.left = layoutParams.left
                child.top = layoutParams.top
                child.right = layoutParams.right
                child.bottom = layoutParams.bottom
            }
        }
    }

    fun addMarker(view: View, left: Int, top: Int, relativeAnchorLeft: Float = -0.5f,
                  relativeAnchorTop: Float = -1f, absoluteAnchorLeft: Float = 0f,
                  absoluteAnchorTop: Float = 0f, fixedAngle: Float? = null) {
        val layoutParams = MarkerLayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                left, top,
                relativeAnchorLeft, relativeAnchorTop,
                absoluteAnchorLeft, absoluteAnchorTop, fixedAngle)
        addView(view, layoutParams)
    }

    fun addCallout(view: View, left: Int, top: Int, relativeAnchorLeft: Float = -0.5f,
                   relativeAnchorTop: Float = -1f, absoluteAnchorLeft: Float = 0f,
                   absoluteAnchorTop: Float = 0f) {
        addMarker(view, left, top, relativeAnchorLeft, relativeAnchorTop, absoluteAnchorLeft,
                absoluteAnchorTop)
        calloutViewList.add(view)
    }

    fun removeMarker(view: View) {
        if (view.parent === this) {
            removeView(view)
        }
    }

    fun removeCallout(view: View) {
        removeMarker(view)
        calloutViewList.remove(view)
    }

    fun removeAllCallout() {
        calloutViewList.forEach {
            removeViewInLayout(it)
        }
        requestLayout()
        invalidate()
    }

    fun moveMarker(view: View, x: Int, y: Int, angle: Float?) {
        val lp = view.layoutParams as? MarkerLayoutParams ?: return
        lp.x = x
        lp.y = y
        lp.fixedAngle = angle
        view.layoutParams = lp
        requestLayout()
    }

    fun rotateMarker(view: View, angle: Float) {
        val lp = view.layoutParams as? MarkerLayoutParams ?: return
        lp.fixedAngle = angle
        view.layoutParams = lp
        requestLayout()
    }

    fun setMarkerTapListener(markerTapListener: MarkerTapListener) {
        this.markerTapListener = markerTapListener
    }

    open fun processHit(x: Int, y: Int) {
        markerTapListener?.let {
            val view = getViewFromTap(x, y)
            if (view != null) {
                it.onMarkerTap(view, x, y)
            }
        }
    }

    internal fun getViewFromTap(x: Int, y: Int): View? {
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            val layoutParams = child.layoutParams as MarkerLayoutParams
            val hitRect = layoutParams.getHitRect()
            if (hitRect.contains(x, y)) {
                return child
            }
        }
        return null
    }
}

internal class MarkerLayoutParams(width: Int, height: Int, var x: Int, var y: Int,
                                  var relativeAnchorX: Float, var relativeAnchorY: Float,
                                  var absoluteAnchorX: Float, var absoluteAnchorY: Float,
                                  var fixedAngle: Float? = null)
                                  : ViewGroup.LayoutParams(width, height) {

    var top: Int = 0
    var left: Int = 0
    var bottom: Int = 0
    var right: Int = 0

    var shouldRotateWithMap: Boolean = fixedAngle != null

    fun getHitRect(): Rect = Rect(left, top, right, bottom)
}

interface MarkerTapListener {
    fun onMarkerTap(view: View, x: Int, y: Int)
}

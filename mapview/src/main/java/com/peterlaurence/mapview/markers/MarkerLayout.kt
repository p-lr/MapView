package com.peterlaurence.mapview.markers

import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import com.peterlaurence.mapview.MapView

/**
 * All markers are laid out using this view.
 * All apis related to makers are implemented as extension functions on the [MapView], like for
 * example [MapView.addMarker].
 *
 * @author peterLaurence on 18/06/2019
 */
open class MarkerLayout(context: Context) : ViewGroup(context) {

    private var mScale = 1f
    private var markerTapListener: MarkerTapListener? = null
    private val calloutViewList = mutableListOf<View>()

    init {
        clipChildren = false
    }

    fun setScale(scale: Float) {
        mScale = scale
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        measureChildren(widthMeasureSpec, heightMeasureSpec)
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            populateLayoutParams(child)
        }
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(availableWidth, availableHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val layoutParams = child.layoutParams as MarkerLayoutParams
                child.layout(layoutParams.mLeft, layoutParams.mTop, layoutParams.mRight, layoutParams.mBottom)
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
            val scaledX = (layoutParams.x * mScale).toInt()
            val scaledY = (layoutParams.y * mScale).toInt()
            // save computed values
            layoutParams.mLeft = (scaledX + widthOffset).toInt()
            layoutParams.mTop = (scaledY + heightOffset).toInt()
            layoutParams.mRight = layoutParams.mLeft + actualWidth
            layoutParams.mBottom = layoutParams.mTop + actualHeight
        }
        return layoutParams
    }

    fun addMarker(view: View, left: Int, top: Int, relativeAnchorLeft: Float = -0.5f,
                  relativeAnchorTop: Float = -1f, absoluteAnchorLeft: Float = 0f,
                  absoluteAnchorTop: Float = 0f) {
        val layoutParams = MarkerLayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                left, top,
                relativeAnchorLeft, relativeAnchorTop,
                absoluteAnchorLeft, absoluteAnchorTop)
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

    fun moveMarker(view: View, x: Int, y: Int) {
        val lp = view.layoutParams as? MarkerLayoutParams ?: return
        lp.x = x
        lp.y = y
        view.layoutParams = lp
        requestLayout()
    }

    fun setMarkerTapListener(markerTapListener: MarkerTapListener) {
        this.markerTapListener = markerTapListener
    }

    fun processHit(x: Int, y: Int) {
        markerTapListener?.let {
            val view = getViewFromTap(x, y)
            if (view != null) {
                it.onMarkerTap(view, x, y)
            }
        }
    }

    private fun getViewFromTap(x: Int, y: Int): View? {
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

private class MarkerLayoutParams(width: Int, height: Int, var x: Int, var y: Int, var relativeAnchorX: Float, var relativeAnchorY: Float, var absoluteAnchorX: Float, var absoluteAnchorY: Float) : ViewGroup.LayoutParams(width, height) {

    var mTop: Int = 0
    var mLeft: Int = 0
    var mBottom: Int = 0
    var mRight: Int = 0

    fun getHitRect(): Rect = Rect(mLeft, mTop, mRight, mBottom)
}

interface MarkerTapListener {
    fun onMarkerTap(view: View, x: Int, y: Int)
}

/**
 * Add a marker to the the MapView.  The marker can be any View.
 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters.
 *
 * @param view    View instance to be added to the TileView.
 * @param x       Relative x position the View instance should be positioned at.
 * @param y       Relative y position the View instance should be positioned at.
 * @param relativeAnchorLeft The x-axis position of a marker will be offset by a number equal to the width of the marker multiplied by this value.
 * @param relativeAnchorTop  The y-axis position of a marker will be offset by a number equal to the height of the marker multiplied by this value.
 * @param absoluteAnchorLeft The x-axis position of a marker will be offset by this value.
 * @param absoluteAnchorTop  The y-axis position of a marker will be offset by this value.
 */
fun MapView.addMarker(view: View, x: Double, y: Double, relativeAnchorLeft: Float = -0.5f,
                      relativeAnchorTop: Float = -1f, absoluteAnchorLeft: Float = 0f,
                      absoluteAnchorTop: Float = 0f) {

    markerLayout.addMarker(view,
            coordinateTranslater.translateX(x),
            coordinateTranslater.translateY(y),
            relativeAnchorLeft, relativeAnchorTop,
            absoluteAnchorLeft, absoluteAnchorTop
    )
}

/**
 * Set a MarkerTapListener for the MapView instance (rather than on a single marker view).
 * Unlike standard touch events attached to marker View's (e.g., View.OnClickListener),
 * MarkerTapListener.onMarkerTapEvent does not consume the touch event, so will not interfere
 * with scrolling.
 */
fun MapView.setMarkerTapListener(markerTapListener: MarkerTapListener) {
    markerLayout.setMarkerTapListener(markerTapListener)
}

/**
 * Moves an existing marker to another position.
 *
 * @param view The marker View to be repositioned.
 * @param x    Relative x position the View instance should be positioned at.
 * @param y    Relative y position the View instance should be positioned at.
 */
fun MapView.moveMarker(view: View, x: Double, y: Double) {
    markerLayout.moveMarker(view,
            coordinateTranslater.translateX(x),
            coordinateTranslater.translateY(y))
}

/**
 * Scroll the TileView so that the View passed is centered in the viewport.
 *
 * @param view          The View marker that the TileView should center on.
 * @param shouldAnimate True if the movement should use a transition effect.
 */
fun MapView.moveToMarker(view: View, shouldAnimate: Boolean) {
    if (markerLayout.indexOfChild(view) == -1) {
        throw IllegalStateException("The view passed is not an existing marker")
    }
    val params = view.layoutParams
    if (params is MarkerLayoutParams) {
        val scaledX = (params.x * scale).toInt()
        val scaledY = (params.y * scale).toInt()
        if (shouldAnimate) {
            slideToAndCenter(scaledX, scaledY)
        } else {
            scrollToAndCenter(scaledX, scaledY)
        }
    }
}

/**
 * Removes a marker View from the TileView's view tree.
 *
 * @param view The marker View to be removed.
 */
fun MapView.removeMarker(view: View) {
    markerLayout.removeMarker(view)
}

/**
 * Add a callout to the the MapView.  The callout can be any View.
 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters.
 *
 * @param view    View instance to be added to the MapView.
 * @param x       Relative x position the View instance should be positioned at.
 * @param y       Relative y position the View instance should be positioned at.
 * @param relativeAnchorLeft The x-axis position of a marker will be offset by a number equal to the width of the marker multiplied by this value.
 * @param relativeAnchorTop  The y-axis position of a marker will be offset by a number equal to the height of the marker multiplied by this value.
 * @param absoluteAnchorLeft The x-axis position of a marker will be offset by this value.
 * @param absoluteAnchorTop  The y-axis position of a marker will be offset by this value.
 */
fun MapView.addCallout(view: View, x: Double, y: Double, relativeAnchorLeft: Float = -0.5f,
                       relativeAnchorTop: Float = -1f, absoluteAnchorLeft: Float = 0f,
                       absoluteAnchorTop: Float = 0f) {
    markerLayout.addCallout(view,
            coordinateTranslater.translateX(x),
            coordinateTranslater.translateY(y),
            relativeAnchorLeft, relativeAnchorTop,
            absoluteAnchorLeft, absoluteAnchorTop
    )
}

/**
 * Removes a callout View from the MapView's view tree.
 *
 * @param view The callout View to be removed.
 */
fun MapView.removeCallout(view: View) {
    markerLayout.removeCallout(view)
}
package com.peterlaurence.mapview.api

import android.view.View
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.markers.MarkerLayoutParams
import com.peterlaurence.mapview.markers.MarkerTapListener

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
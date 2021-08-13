@file:Suppress("unused")

package ovh.plrapps.mapview.api

import android.view.View
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.markers.MarkerLayoutParams
import ovh.plrapps.mapview.markers.MarkerTapListener
import kotlin.math.max
import kotlin.math.min

/**
 * Add a marker to the the MapView.  The marker can be any View.
 * No LayoutParams are required; the View will be laid out using WRAP_CONTENT for both width and height, and positioned based on the parameters.
 *
 * @param view    View instance to be added to the MapView.
 * @param x       Relative x position the View instance should be positioned at.
 * @param y       Relative y position the View instance should be positioned at.
 * @param relativeAnchorLeft The x-axis position of a marker will be offset by a number equal to the width of the marker multiplied by this value.
 * @param relativeAnchorTop  The y-axis position of a marker will be offset by a number equal to the height of the marker multiplied by this value.
 * @param absoluteAnchorLeft The x-axis position of a marker will be offset by this value.
 * @param absoluteAnchorTop  The y-axis position of a marker will be offset by this value.
 * @param tag  An optional tag, to later retrieve the marker using [getMarkerByTag].
 */
fun MapView.addMarker(view: View, x: Double, y: Double, relativeAnchorLeft: Float = -0.5f,
                      relativeAnchorTop: Float = -1f, absoluteAnchorLeft: Float = 0f,
                      absoluteAnchorTop: Float = 0f, tag: String? = null) {

    val coordinateTranslater = coordinateTranslater ?: return
    markerLayout?.addMarker(view,
            coordinateTranslater.translateX(x),
            coordinateTranslater.translateY(y),
            relativeAnchorLeft, relativeAnchorTop,
            absoluteAnchorLeft, absoluteAnchorTop,
            tag
    )
}

/**
 * If a marker was added with a tag, it can be retrieved with this method.
 */
fun MapView.getMarkerByTag(tag: String): View? {
    return markerLayout?.getMarkerByTag(tag)
}

/**
 * Set a [MarkerTapListener] to the MapView (rather than on a single marker view).
 * Unlike standard touch events attached to marker View's (e.g., [View.OnClickListener]),
 * [MarkerTapListener.onMarkerTap] does not consume the touch event, so will not interfere
 * with scrolling.
 */
fun MapView.setMarkerTapListener(markerTapListener: MarkerTapListener) {
    markerLayout?.setMarkerTapListener(markerTapListener)
}

/**
 * Moves an existing marker to the specified position.
 *
 * @param view The marker View to be repositioned.
 * @param x    Relative x position the View instance should be positioned at.
 * @param y    Relative y position the View instance should be positioned at.
 */
fun MapView.moveMarker(view: View, x: Double, y: Double) {
    val coordinateTranslater = coordinateTranslater ?: return
    markerLayout?.moveMarker(view,
            coordinateTranslater.translateX(x),
            coordinateTranslater.translateY(y))
}

/**
 * Moves an existing maker to the specified position, while constraining its position to the inner
 * bounds of the [MapView].
 *
 * @param view The marker View to be repositioned.
 * @param x    Relative x position the View instance should be positioned at.
 * @param y    Relative y position the View instance should be positioned at.
 */
fun MapView.moveMarkerConstrained(view: View, x: Double, y: Double) {
    val markerLayout = markerLayout ?: return
    val coordinateTranslater = coordinateTranslater ?: return

    val l = coordinateTranslater.left
    val r = coordinateTranslater.right
    val t = coordinateTranslater.top
    val b = coordinateTranslater.bottom

    val xConstrained = x.coerceIn(min(l, r), max(l, r))
    val yConstrained = y.coerceIn(min(b, t), max(b, t))

    markerLayout.moveMarker(view,
            coordinateTranslater.translateX(xConstrained),
            coordinateTranslater.translateY(yConstrained))
}

/**
 * Finds view which bounding box contains position.
 *
 * @param x    Relative x position the View instance should be positioned at.
 * @param y    Relative y position the View instance should be positioned at.
 */
fun MapView.getMarkerFromPosition(x: Double, y: Double): View? {
    val coordinateTranslater = coordinateTranslater ?: return null
    val xPixel = coordinateTranslater.translateAndScaleX(x, scale)
    val yPixel = coordinateTranslater.translateAndScaleY(y, scale)
    return markerLayout?.getViewFromTap(xPixel, yPixel)
}

/**
 * Scrolls the MapView so that the passed View is centered in the viewport.
 * The scale remains constant.
 *
 * The scroll position is animated if [shouldAnimate] is set to `true`.
 *
 * @param view The marker that the MapView should center on.
 * @param shouldAnimate `true` if the movement should use a transition effect.
 */
fun MapView.moveToMarker(view: View, shouldAnimate: Boolean) {
    moveToMarker(view, scale, shouldAnimate)
}

/**
 * Scrolls the MapView so that the marker passed is centered in the viewport.
 * Takes an additional [destinationScale] parameter.
 *
 * The scroll position and scale are animated if [shouldAnimate] is set to `true`.
 *
 * @param view The marker that the MapView should center on.
 * @param destinationScale The scale of the MapView when centered on the marker
 * @param shouldAnimate True if the movement should use a transition effect.
 */
fun MapView.moveToMarker(view: View, destinationScale: Float, shouldAnimate: Boolean) {
    if (markerLayout?.indexOfChild(view) == -1) {
        throw IllegalStateException("The view passed is not an existing marker")
    }
    val params = view.layoutParams
    if (params is MarkerLayoutParams) {
        val scaledX = (params.x * destinationScale).toInt()
        val scaledY = (params.y * destinationScale).toInt()
        /* Internally, dimensions (width and height) may not be set yet.
         * Schedule the scroll after the next layout pass. */
        post {
            if (shouldAnimate) {
                slideToAndCenterWithScale(scaledX, scaledY, destinationScale)
            } else {
                scale = destinationScale
                scrollToAndCenter(scaledX, scaledY)
            }
        }
    }
}

/**
 * Removes a marker from MapView's view hierarchy.
 *
 * @param view The marker to be removed.
 */
fun MapView.removeMarker(view: View) {
    view.clearAnimation()
    markerLayout?.removeMarker(view)
}

/**
 * Add a callout to the MapView. The callout can be any View.
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
    val coordinateTranslater = coordinateTranslater ?: return
    markerLayout?.addCallout(view,
            coordinateTranslater.translateX(x),
            coordinateTranslater.translateY(y),
            relativeAnchorLeft, relativeAnchorTop,
            absoluteAnchorLeft, absoluteAnchorTop
    )
}

/**
 * Removes a callout View from MapView's view hierarchy.
 *
 * @param view The callout View to be removed.
 */
fun MapView.removeCallout(view: View) {
    view.clearAnimation()
    markerLayout?.removeCallout(view)
}

/**
 * Removes all callouts from MapView's view hierarchy.
 */
fun MapView.removeAllCallouts() {
    markerLayout?.removeAllCallout()
}
@file:Suppress("unused")

package ovh.plrapps.mapview.api

import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.core.Viewport
import ovh.plrapps.mapview.util.AngleDegree
import kotlin.math.max
import kotlin.math.min

/**
 * Adds extra padding around the map, making it possible to scroll past the end of the border
 * even when zoomed in.
 *
 * @param padding  Additional empty padding in pixels when at scale 1f.
 */
fun MapView.setBasePadding(padding: Int) {
    gestureController.setBasePadding(padding)
}

/**
 * Programmatically set the rotation angle of the MapView, in decimal degrees.
 * It should be called after the [MapView] configuration and after the [MapView] has been laid out.
 * Attempts to set the angle before [MapView] has been laid out will be ignored.
 */
fun MapView.setAngle(angle: AngleDegree) {
    if (!gestureController.isLayoutDone) return
    if (gestureController.rotationEnabled) {
        gestureController.angle = angle
    }
}

/**
 * Constrain the scroll to the specified relative limits. The [MapView.defineBounds] method *must*
 * have been previously invoked (otherwise relative values have no meaning).
 * To enforce that the viewport is constrained to the specified area, the minimum scale mode is set
 * to [MinimumScaleMode.FILL].
 */
fun MapView.constrainScroll(relativeMinX: Double, relativeMinY: Double, relativeMaxX: Double,
                            relativeMaxY: Double) {
    gestureController.setMinimumScaleMode(MinimumScaleMode.FILL)
    val coordinateTranslater = coordinateTranslater ?: return
    gestureController.setScrollLimits(
            coordinateTranslater.translateX(relativeMinX),
            coordinateTranslater.translateY(relativeMinY),
            coordinateTranslater.translateX(relativeMaxX),
            coordinateTranslater.translateY(relativeMaxY)
    )
}

/**
 * Dynamically enable rotation.
 * By default, the rotation gestures are handled. If you want to set the angle programmatically only,
 * set [handleRotationGesture] to false.
 */
fun MapView.enableRotation(handleRotationGesture: Boolean = true) {
    gestureController.rotationEnabled = true
    gestureController.handleRotationGesture = handleRotationGesture
}

/**
 * Dynamically disable rotation.
 * By default, when the rotation is disabled, the [MapView] keeps the last angle value ([freezeAngle]).
 * You can optionally set a value to [freezeAngle], so that the supplied value is guarantied to be
 * accounted for.
 */
fun MapView.disableRotation(freezeAngle: AngleDegree? = null) {
    gestureController.rotationEnabled = false
    if (freezeAngle != null) {
        /* Force the angle value, even if the rotation is disabled */
        gestureController.angle = freezeAngle
    }
}

/**
 * Given a relative coordinate in X, get the constrained value in the bounds of the [MapView].
 */
fun MapView.getConstrainedX(x: Double): Double? {
    val coordinateTranslater = coordinateTranslater ?: return null
    val l = coordinateTranslater.left
    val r = coordinateTranslater.right
    return x.coerceIn(min(l, r), max(l, r))
}

/**
 * Given a relative coordinate in Y, get the constrained value in the bounds of the [MapView].
 */
fun MapView.getConstrainedY(y: Double): Double? {
    val coordinateTranslater = coordinateTranslater ?: return null
    val t = coordinateTranslater.top
    val b = coordinateTranslater.bottom
    return y.coerceIn(min(b, t), max(b, t))
}

/**
 * Get a copy of the area taken into account to fetch tiles.
 * This area is larger than the visible viewport when positive padding is set in the [MapView]
 * configuration.
 */
fun MapView.getViewport(): Viewport {
    return viewport.copy()
}

/**
 * Get a copy of the visible area.
 */
fun MapView.getVisibleViewport(): Viewport {
    val padding = configuration.padding
    return viewport.copy(left = viewport.left + padding, top = viewport.top + padding,
            right = viewport.right - padding, bottom = viewport.bottom - padding)
}

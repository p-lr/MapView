package com.peterlaurence.mapview.api

import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.util.AngleDegree

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
    gestureController.angle = angle
}
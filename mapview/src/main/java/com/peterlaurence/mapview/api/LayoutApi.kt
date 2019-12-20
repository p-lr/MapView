package com.peterlaurence.mapview.api

import com.peterlaurence.mapview.layout.GestureLayout

/**
 * Adds extra padding around the map, making it possible to scroll past the end of the border
 * even when zoomed in.
 *
 * @param padding  Additional empty padding in pixels when at scale 1f.
 */
fun GestureLayout.setBasePadding(padding: Int) {
    gestureController.setBasePadding(padding)
}
package com.peterlaurence.mapview.core

/**
 * Denotes the visible area on the screen. Values are in pixels.
 */
data class Viewport(var left: Int = 0, var top: Int = 0, var right: Int = 0, var bottom: Int = 0) {
    fun scale(scale: Double) {
        left *= (left * scale).toInt()
        top *= (top * scale).toInt()
        right *= (right * scale).toInt()
        bottom *= (bottom * scale).toInt()
    }

    fun set(left: Int, top: Int, right: Int, bottom: Int) {
        this.left = left
        this.top = top
        this.right = right
        this.bottom = bottom
    }
}
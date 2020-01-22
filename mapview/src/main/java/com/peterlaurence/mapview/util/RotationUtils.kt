package com.peterlaurence.mapview.util

typealias AngleDegree = Float
typealias AngleRad = Float

fun AngleDegree.toRad(): Float = Math.toRadians(this.toDouble()).toFloat()

/**
 * Constrain the angle to have values between 0f and 360f.
 */
fun AngleDegree.modulo(): AngleDegree {
    val mod = this % 360f
    return if (mod < 0) {
        mod + 360f
    } else mod
}


package com.peterlaurence.mapview.layout.controllers

typealias AngleDegree = Float
typealias AngleRad = Float

/**
 * Constrain the angle to have values between 0f and 360f.
 */
fun AngleDegree.modulo(): AngleDegree {
    val mod = this % 360f
    return if (mod < 0) { mod + 360f } else mod
}

fun AngleDegree.addModulo(angle: AngleDegree): AngleDegree {
    return (this + angle).modulo()
}


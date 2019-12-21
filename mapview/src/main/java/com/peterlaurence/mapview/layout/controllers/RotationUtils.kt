package com.peterlaurence.mapview.layout.controllers

typealias AngleDegree = Float
typealias AngleRad = Float

/**
 * Constrain the angle to have values between 0f and 180f.
 */
fun AngleDegree.modulo(): AngleDegree {
    val mod = this % 180f
    return if (mod < 0) { mod + 180f } else mod
}

fun AngleDegree.addModulo(angle: AngleDegree): AngleDegree {
    return (this + angle).modulo()
}


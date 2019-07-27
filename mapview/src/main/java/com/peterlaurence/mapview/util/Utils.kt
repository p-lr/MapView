package com.peterlaurence.mapview.util

fun scale(base: Int, multiplier: Float): Int = (base * multiplier + 0.5f).toInt()
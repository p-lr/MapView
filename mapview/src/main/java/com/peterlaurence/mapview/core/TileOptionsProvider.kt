package com.peterlaurence.mapview.core

import android.graphics.ColorFilter

/**
 * Tile rendering options provider. Optional parameter for
 * [com.peterlaurence.mapview.MapViewConfiguration].
 */
interface TileOptionsProvider {
    /* Must not be a blocking call - should return immediately */
    @JvmDefault
    fun getColorFilter(row: Int, col: Int, zoomLvl: Int): ColorFilter? = null
}
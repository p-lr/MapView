package ovh.plrapps.mapview.core

import android.graphics.ColorFilter

/**
 * Tile rendering options provider. Optional parameter for
 * [ovh.plrapps.mapview.MapViewConfiguration].
 */
interface TileOptionsProvider {
    /* Must not be a blocking call - should return immediately */
    fun getColorFilter(row: Int, col: Int, zoomLvl: Int): ColorFilter? = null

    /**
     * Controls the speed of fade in effect when rendering tiles. Higher values make alpha
     * value go to 255 faster. Should be in the range [0.0f, 1.0f].
     */
    val alphaTick : Float
        get() = 0.07f
}
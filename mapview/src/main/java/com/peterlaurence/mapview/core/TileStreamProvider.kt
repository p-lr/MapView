package com.peterlaurence.mapview.core

import java.io.InputStream

/**
 * The tile provider that should only be used by MapView internals.
 */
interface TileStreamProvider {
    fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream?
}
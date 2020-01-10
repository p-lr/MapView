package com.peterlaurence.mapview.core

import kotlin.math.*

/**
 * Resolves the visible tiles
 *
 * @param levelCount Number of levels
 * @param fullWidth Width of the map at scale 1.0f
 * @param fullHeight Height of the map at scale 1.0f
 * @param magnifyingFactor Alters the level at which tiles are picked for a given scale. By default,
 * the level immediately higher (in index) is picked, to avoid sub-sampling. This corresponds to a
 * [magnifyingFactor] of 0. The value 1 will result in picking the current level at a given scale,
 * which will be at a relative scale between 1.0 and 2.0
 *
 * @author peterLaurence on 25/05/2019
 */
class VisibleTilesResolver(private val levelCount: Int, private val fullWidth: Int,
                           private val fullHeight: Int, private val tileSize: Int = 256,
                           private val magnifyingFactor: Int = 0) {

    private var scale: Float = 1.0f
    var currentLevel = levelCount - 1
        private set
    var subSample: Int = 0
        private set

    /**
     * Last level is at scale 1.0f, others are at scale 1.0 / power_of_2
     */
    private val scaleForLevel = (0 until levelCount).associateWith {
        (1.0 / 2.0.pow((levelCount - it - 1).toDouble())).toFloat()
    }

    fun setScale(scale: Float) {
        this.scale = scale

        this.subSample = if (scale < scaleForLevel[0] ?: Float.MIN_VALUE) {
            ceil(ln((scaleForLevel[0] ?: error("")).toDouble() / scale) / ln(2.0)).toInt()
        } else {
            0
        }

        /* Update current level */
        currentLevel = getLevel(scale, magnifyingFactor)
    }

    /**
     * Get the scale for a given [level] (also called zoom).
     * @return the scale or null if no such level was configured.
     */
    fun getScaleForLevel(level: Int): Float? {
        return scaleForLevel[level]
    }

    /**
     * Returns the level, an entire value belonging to [0 ; [levelCount] - 1]
     */
    private fun getLevel(scale: Float, magnifyingFactor: Int = 0): Int {
        /* This value can be negative */
        val partialLevel = levelCount - 1 - magnifyingFactor +
                ln(scale.toDouble()) / ln(2.0)

        /* The level can't be greater than levelCount - 1.0 */
        val capedLevel = min(partialLevel, levelCount - 1.0)

        /* The level can't be lower than 0 */
        return ceil(max(capedLevel, 0.0)).toInt()
    }

    /**
     * Get the [VisibleTiles], given the visible area in pixels.
     *
     * @param viewport The [Viewport] which represents the visible area. Its values depend on the
     * scale.
     */
    fun getVisibleTiles(viewport: Viewport, level: Int = currentLevel): VisibleTiles {
        val scaleAtLevel = scaleForLevel[level] ?: throw AssertionError()
        val relativeScale = scale / scaleAtLevel

        /* At the current level, row and col index have maximum values */
        val maxCol = max(0.0, ceil(fullWidth * scaleAtLevel.toDouble() / tileSize) - 1).toInt()
        val maxRow = max(0.0, ceil(fullHeight * scaleAtLevel.toDouble() / tileSize) - 1).toInt()

        fun Int.lowerThan(limit: Int): Int {
            return if (this <= limit) this else limit
        }

        val scaledTileSize = tileSize.toDouble() * relativeScale

        val colLeft = floor(viewport.left / scaledTileSize).toInt().lowerThan(maxCol)
        val rowTop = floor(viewport.top / scaledTileSize).toInt().lowerThan(maxRow)
        val colRight = (ceil(viewport.right / scaledTileSize).toInt() - 1).lowerThan(maxCol)
        val rowBottom = (ceil(viewport.bottom / scaledTileSize).toInt() - 1).lowerThan(maxRow)

        val tileMatrix = (rowTop..rowBottom).associateWith {
            colLeft..colRight
        }
        val count = (rowBottom - rowTop + 1) * (colRight - colLeft + 1)

        return VisibleTiles(level, tileMatrix, count, subSample)
    }
}

/**
 * Properties container for the computed visible tiles.
 * @param level 0-based level index
 * @param tileMatrix contains all (row, col) indexes, grouped by rows
 * @param count the precomputed total count
 * @param subSample the current sub-sample factor. If the current scale of the [VisibleTilesResolver]
 * is lower than the scale of the minimum level, [subSample] is greater than 0. Otherwise, [subSample]
 * equals 0.
 */
data class VisibleTiles(var level: Int, val tileMatrix: TileMatrix, val count: Int, val subSample: Int = 0)

typealias Row = Int
typealias ColRange = IntRange
typealias TileMatrix = Map<Row, ColRange>
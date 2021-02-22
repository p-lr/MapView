package ovh.plrapps.mapview.core

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.pow

class VisibleTilesResolverTest {

    @Test
    fun levelTest() {
        val resolver = VisibleTilesResolver(8, 1000, 800)

        assertEquals(7, resolver.currentLevel)
        resolver.setScale(0.7f)
        assertEquals(7, resolver.currentLevel)
        resolver.setScale(0.5f)
        assertEquals(6, resolver.currentLevel)
        resolver.setScale(0.26f)
        assertEquals(6, resolver.currentLevel)
        resolver.setScale(0.15f)
        assertEquals(5, resolver.currentLevel)
        resolver.setScale(0.0078f)
        assertEquals(0, resolver.currentLevel)
        resolver.setScale(0.008f)
        assertEquals(1, resolver.currentLevel)

        /* Outside of bounds test */
        resolver.setScale(0.0030f)
        assertEquals(0, resolver.currentLevel)
        resolver.setScale(1f)
        assertEquals(7, resolver.currentLevel)
    }

    @Test
    fun subSampleTest() {
        val resolver = VisibleTilesResolver(8, 1000, 800)

        resolver.setScale(0.008f)
        assertEquals(0, resolver.subSample)
        resolver.setScale(0.0078f)  // 0.0078 is the scale of level 0
        assertEquals(1, resolver.subSample)

        /* Outside of bounds: subsample should be at least 1 */
        resolver.setScale((1.0 / 2.0.pow(7.5)).toFloat())
        assertEquals(1, resolver.subSample)
        resolver.setScale((1.0 / 2.0.pow(9)).toFloat())
        assertEquals(2, resolver.subSample)
        resolver.setScale((1.0 / 2.0.pow(10)).toFloat())
        assertEquals(3, resolver.subSample)
    }

    @Test
    fun viewportTestSimple() {
        val resolver = VisibleTilesResolver(3, 1000, 800)
        var viewport = Viewport(0, 0, 700, 512)

        var visibleTiles = resolver.getVisibleTiles(viewport)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(2, visibleTiles.level)
            assertEquals(0, colLeft)
            assertEquals(0, rowTop)
            assertEquals(2, colRight)
            assertEquals(1, rowBottom)
        }


        resolver.setScale(0.5f)
        viewport = Viewport(0, 0, 512, 512)
        visibleTiles = resolver.getVisibleTiles(viewport)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(1, visibleTiles.level)
            assertEquals(0, colLeft)
            assertEquals(0, rowTop)
            assertEquals(1, colRight)
            assertEquals(1, rowBottom)
        }


        val resolver2 = VisibleTilesResolver(5, 8192, 8192)
        val viewport2 = Viewport(0, 0, 8192, 8192)
        visibleTiles = resolver2.getVisibleTiles(viewport2)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(4, visibleTiles.level)
            assertEquals(0, colLeft)
            assertEquals(0, rowTop)
            assertEquals(31, colRight)
            assertEquals(31, rowBottom)
        }
    }

    @Test
    fun viewportTestAdvanced() {
        // 6-level map.
        // 256 * 2⁶ = 16384
        val resolver = VisibleTilesResolver(6, 16400, 8000)
        var viewport = Viewport(0, 0, 1080, 1380)
        var visibleTiles = resolver.getVisibleTiles(viewport)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(5, visibleTiles.level)
            assertEquals(0, colLeft)
            assertEquals(0, rowTop)
            assertEquals(4, colRight)
            assertEquals(5, rowBottom)
        }

        viewport = Viewport(4753, 6222, 4753 + 1080, 6222 + 1380)
        visibleTiles = resolver.getVisibleTiles(viewport)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(5, visibleTiles.level)
            assertEquals(18, colLeft)
            assertEquals(24, rowTop)
            assertEquals(22, colRight)
            assertEquals(29, rowBottom)
        }

        viewport = Viewport(3720, 1543, 3720 + 1080, 1543 + 1380)
        resolver.setScale(0.5f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(4, visibleTiles.level)
            assertEquals(14, colLeft)
            assertEquals(6, rowTop)
            assertEquals(18, colRight)
            assertEquals(11, rowBottom)
        }

        viewport = Viewport(3720, 1543, 3720 + 1080, 1543 + 1380)
        resolver.setScale(0.71f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(5, visibleTiles.level)
            assertEquals(20, colLeft)
            assertEquals(8, rowTop)
            assertEquals(26, colRight)
            assertEquals(16, rowBottom)
        }

        viewport = Viewport(1643, 427, 1643 + 1080, 427 + 1380)
        resolver.setScale(0.43f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(4, visibleTiles.level)
            assertEquals(7, colLeft)
            assertEquals(1, rowTop)
            assertEquals(12, colRight)
            assertEquals(8, rowBottom)
        }
    }

    @Test
    fun viewportMagnifyingTest() {
        // 6-level map.
        // 256 * 2⁶ = 16384
        var resolver = VisibleTilesResolver(6, 16400, 8000, magnifyingFactor = 1)
        resolver.setScale(0.37f)
        var viewport = Viewport(3720, 1543, 3720 + 1080, 1543 + 1380)
        var visibleTiles = resolver.getVisibleTiles(viewport)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(3, visibleTiles.level)
            assertEquals(9, colLeft)
            assertEquals(4, rowTop)
            assertEquals(12, colRight)
            assertEquals(7, rowBottom)
        }

        // magnify even further, with an abnormally big viewport
        resolver = VisibleTilesResolver(6, 16400, 8000, magnifyingFactor = 2)
        viewport = Viewport(250, 123, 250 + 1080, 123 + 1380)
        resolver.setScale(0.37f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(2, visibleTiles.level)
            assertEquals(0, colLeft)
            assertEquals(0, rowTop)
            assertEquals(1, colRight)
            assertEquals(1, rowBottom)
        }

        // (un)magnify
        resolver = VisibleTilesResolver(6, 16400, 8000, magnifyingFactor = -1)
        viewport = Viewport(3720, 1543, 3720 + 1080, 1543 + 1380)
        resolver.setScale(0.37f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(5, visibleTiles.level)
            assertEquals(39, colLeft)
            assertEquals(16, rowTop)
            assertEquals(50, colRight)
            assertEquals(30, rowBottom)
        }

        // Try to (un)magnify beyond available level: this shouldn't change anything
        resolver = VisibleTilesResolver(6, 16400, 8000, magnifyingFactor = -2)
        viewport = Viewport(3720, 1543, 3720 + 1080, 1543 + 1380)
        resolver.setScale(0.37f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        with(visibleTiles.tileMatrix.toTileRange()) {
            assertEquals(5, visibleTiles.level)
            assertEquals(39, colLeft)
            assertEquals(16, rowTop)
            assertEquals(50, colRight)
            assertEquals(30, rowBottom)
        }
    }
}

private data class TileRange(val colLeft: Int, val rowTop: Int, val colRight: Int, val rowBottom: Int)

/**
 * If the tile matrix represents a rectangle, then is can be represented by a [TileRange].
 * It only makes sense when the angle of rotation is 0 modulo pi/2
 */
private fun TileMatrix.toTileRange(): TileRange {
    val rowTop = keys.minOrNull()!!
    val rowBottom = keys.maxOrNull()!!
    val colRange = getValue(rowTop)
    val colLeft = colRange.first
    val colRight = colRange.last
    return TileRange(colLeft, rowTop, colRight, rowBottom)
}
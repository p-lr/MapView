package com.peterlaurence.mapview.core

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
        var viewport = Viewport(0, 0,700, 512)

        var visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(2, visibleTiles.level)
        assertEquals(0, visibleTiles.colLeft)
        assertEquals(0, visibleTiles.rowTop)
        assertEquals(2, visibleTiles.colRight)
        assertEquals(1, visibleTiles.rowBottom)

        resolver.setScale(0.5f)
        viewport = Viewport(0, 0, 200, 300)
        visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(1, visibleTiles.level)
        assertEquals(0, visibleTiles.colLeft)
        assertEquals(0, visibleTiles.rowTop)
        assertEquals(0, visibleTiles.colRight)
        assertEquals(1, visibleTiles.rowBottom)
    }

    @Test
    fun viewportTestAdvanced() {
        // 6-level map.
        // 256 * 2⁶ = 16384
        val resolver = VisibleTilesResolver(6, 16400, 8000)
        var viewport = Viewport(0, 0, 1080, 1380)
        var visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(5, visibleTiles.level)
        assertEquals(0, visibleTiles.colLeft)
        assertEquals(0, visibleTiles.rowTop)
        assertEquals(4, visibleTiles.colRight)
        assertEquals(5, visibleTiles.rowBottom)

        viewport = Viewport(4753, 6222, 4753 + 1080, 6222 + 1380)
        visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(5, visibleTiles.level)
        assertEquals(18, visibleTiles.colLeft)
        assertEquals(24, visibleTiles.rowTop)
        assertEquals(22, visibleTiles.colRight)
        assertEquals(29, visibleTiles.rowBottom)

        viewport = Viewport(3720, 1543, 3720 + 1080, 1543 + 1380)
        resolver.setScale(0.5f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(4, visibleTiles.level)
        assertEquals(14, visibleTiles.colLeft)
        assertEquals(6, visibleTiles.rowTop)
        assertEquals(18, visibleTiles.colRight)
        assertEquals(11, visibleTiles.rowBottom)

        viewport = Viewport(3720, 1543, 3720 + 1080, 1543 + 1380)
        resolver.setScale(0.71f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(5, visibleTiles.level)
        assertEquals(20, visibleTiles.colLeft)
        assertEquals(8, visibleTiles.rowTop)
        assertEquals(26, visibleTiles.colRight)
        assertEquals(16, visibleTiles.rowBottom)

        viewport = Viewport(1643, 427, 1643 + 1080, 427 + 1380)
        resolver.setScale(0.43f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(4, visibleTiles.level)
        assertEquals(7, visibleTiles.colLeft)
        assertEquals(1, visibleTiles.rowTop)
        assertEquals(12, visibleTiles.colRight)
        assertEquals(8, visibleTiles.rowBottom)
    }

    @Test
    fun viewportMagnifyingTest() {
        // 6-level map.
        // 256 * 2⁶ = 16384
        var resolver = VisibleTilesResolver(6, 16400, 8000, magnifyingFactor = 1)
        resolver.setScale(0.37f)
        var viewport = Viewport(3720, 1543, 3720 + 1080, 1543 + 1380)
        var visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(3, visibleTiles.level)
        assertEquals(9, visibleTiles.colLeft)
        assertEquals(4, visibleTiles.rowTop)
        assertEquals(12, visibleTiles.colRight)
        assertEquals(7, visibleTiles.rowBottom)

        // magnify even further, with an abnormally big viewport
        resolver = VisibleTilesResolver(6, 16400, 8000, magnifyingFactor = 2)
        viewport = Viewport(250, 123, 250 + 1080, 123 + 1380)
        resolver.setScale(0.37f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(2, visibleTiles.level)
        assertEquals(0, visibleTiles.colLeft)
        assertEquals(0, visibleTiles.rowTop)
        assertEquals(1, visibleTiles.colRight)
        assertEquals(1, visibleTiles.rowBottom)

        // (un)magnify
        resolver = VisibleTilesResolver(6, 16400, 8000, magnifyingFactor = -1)
        viewport = Viewport(3720, 1543, 3720 + 1080, 1543 + 1380)
        resolver.setScale(0.37f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(5, visibleTiles.level)
        assertEquals(39, visibleTiles.colLeft)
        assertEquals(16, visibleTiles.rowTop)
        assertEquals(50, visibleTiles.colRight)
        assertEquals(30, visibleTiles.rowBottom)

        // Try to (un)magnify beyond available level: this shouldn't change anything
        resolver = VisibleTilesResolver(6, 16400, 8000, magnifyingFactor = -2)
        viewport = Viewport(3720, 1543, 3720 + 1080, 1543 + 1380)
        resolver.setScale(0.37f)
        visibleTiles = resolver.getVisibleTiles(viewport)
        assertEquals(5, visibleTiles.level)
        assertEquals(39, visibleTiles.colLeft)
        assertEquals(16, visibleTiles.rowTop)
        assertEquals(50, visibleTiles.colRight)
        assertEquals(30, visibleTiles.rowBottom)
    }
}
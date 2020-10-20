package com.peterlaurence.mapview.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import com.peterlaurence.mapview.ReferentialData
import com.peterlaurence.mapview.ReferentialOwner
import com.peterlaurence.mapview.core.Tile
import com.peterlaurence.mapview.core.VisibleTilesResolver
import com.peterlaurence.mapview.viewmodel.TileCanvasViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * This is the view where all tiles are drawn into.
 *
 * @author peterLaurence on 02/06/2019
 */
internal class TileCanvasView(ctx: Context, viewModel: TileCanvasViewModel,
                              private val tileSize: Int,
                              private val visibleTilesResolver: VisibleTilesResolver,
                              scope: CoroutineScope
) : View(ctx), ReferentialOwner {
    override var referentialData = ReferentialData(false)
        set(value) {
            field = value
            invalidate()
        }
    private val alphaTick = viewModel.getAlphaTick()

    private var tilesToRender = listOf<Tile>()
    private val dest = Rect()

    init {
        setWillNotDraw(false)

        scope.launch {
            viewModel.getTilesToRender().collect {
                tilesToRender = it
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        /* If there is rotation data, take it into account */
        val rd = referentialData
        if (rd.rotationEnabled) {
            canvas.rotate(rd.angle, (width * rd.centerX).toFloat(), (height * rd.centerY).toFloat())
        }
        canvas.scale(rd.scale, rd.scale)
        drawTiles(canvas)
        canvas.restore()
    }

    /**
     * Draw tiles, while optimizing alpha-related computations (the alpha progress is indeed
     * a constant (not time based, so in some situations the fade in might take longer for some tiles).
     * But each tile has its own alpha value. If any tile has its alpha less than 255, a redraw is
     * scheduled.
     */
    private fun drawTiles(canvas: Canvas) {
        if (tilesToRender.isEmpty()) return

        var needsAnotherPass = false
        for (tile in tilesToRender) {
            val scaleForLevel = visibleTilesResolver.getScaleForLevel(tile.zoom)
                    ?: continue
            val tileScaled = (tileSize / scaleForLevel).toInt()
            val l = tile.col * tileScaled
            val t = tile.row * tileScaled
            val r = l + tileScaled
            val b = t + tileScaled
            dest.set(l, t, r, b)

            /* If a tile has a paint, compute only once the alphaProgress for this loop */
            val paint = tile.paint?.also {
                if (it.alpha < 255) {
                    it.updateAlpha(alphaTick).let { a ->
                        needsAnotherPass = needsAnotherPass || (a < 255)
                    }
                }
            }

            canvas.drawBitmap(tile.bitmap, null, dest, paint)
        }

        /* If at least one tile wasn't fully drawn (alpha < 255), redraw */
        if (needsAnotherPass) {
            invalidate()
        }
    }

    /**
     * Increase the alpha, but don't exceed 255.
     * @return its new value
     */
    private fun Paint.updateAlpha(alphaProgress: Float): Int {
        val newAlpha = alpha + (255 * alphaProgress).toInt()
        alpha = min(255, newAlpha)
        return alpha
    }
}
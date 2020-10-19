package com.peterlaurence.mapview.viewmodel

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.peterlaurence.mapview.core.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * The view-model which contains all the logic related to [Tile] management.
 * It defers [Tile] loading to the [TileCollector].
 *
 * @author peterLaurence on 04/06/2019
 */
internal class TileCanvasViewModel(parentScope: CoroutineScope, tileSize: Int,
                                   private val visibleTilesResolver: VisibleTilesResolver,
                                   tileStreamProvider: TileStreamProvider,
                                   private val tileOptionsProvider: TileOptionsProvider,
                                   workerCount: Int) {

    private val scope = CoroutineScope(parentScope.coroutineContext + Executors.newSingleThreadExecutor().asCoroutineDispatcher())
    private val tilesToRenderLiveData = MutableLiveData<List<Tile>>()
    private val renderTask = scope.throttle(wait = 15) {
        /* Right before sending tiles to the view, reorder them so that tiles from current level are
         * above others */
        val tilesToRenderCopy = tilesToRender.sortedBy {
            it.zoom == lastVisible.level && it.subSample == lastVisible.subSample
        }
        tilesToRenderLiveData.postValue(tilesToRenderCopy)
    }

    private val bitmapPool = Pool<Bitmap>()
    private val paintPool = Pool<Paint>()
    private val visibleTileLocationsChannel = Channel<TileSpec>(capacity = Channel.RENDEZVOUS)
    private val tilesOutput = Channel<Tile>(capacity = Channel.RENDEZVOUS)
    private val visibleTilesFlow = MutableStateFlow<VisibleTiles?>(null)

    /**
     * A [Flow] of [Bitmap] that first collects from the [bitmapPool] on the Main thread. If the
     * pool was empty, a new [Bitmap] is allocated from the calling thread. It's a simple way to
     * share data between coroutines in a thread safe way, using cold flows.
     */
    private val bitmapFlow: Flow<Bitmap> = flow {
        val bitmap = bitmapPool.get()
        emit(bitmap)
    }.flowOn(Dispatchers.Main).map {
        it ?: Bitmap.createBitmap(tileSize, tileSize, Bitmap.Config.RGB_565)
    }

    private lateinit var lastViewport: Viewport
    private lateinit var lastVisible: VisibleTiles
    private var lastVisibleCount: Int = 0
    private var idle = false

    /**
     * So long as this debounced channel is offered a message, the lambda isn't called.
     */
    private val idleDebounced = scope.debounce<Unit>(300) {
        idle = true
        evictTiles(lastVisible)
    }

    private var tilesToRender = mutableListOf<Tile>()

    init {
        /* Collect visible tiles and send specs to the TileCollector */
        scope.launch {
            collectNewTiles()
        }

        /* Launch the TileCollector */
        with(TileCollector(workerCount)) {
            scope.collectTiles(visibleTileLocationsChannel, tilesOutput, tileStreamProvider, bitmapFlow)
        }

        /* Launch a coroutine to consume the produced tiles */
        scope.launch {
            consumeTiles(tilesOutput)
        }
    }

    fun getTilesToRender(): LiveData<List<Tile>> {
        return tilesToRenderLiveData
    }

    fun getAlphaTick(): Float {
        return tileOptionsProvider.alphaTick
    }

    fun setViewport(viewport: Viewport) = scope.launch {
        /* It's important to set the idle flag to false before launching computations, so that
         * tile eviction don't happen too quickly (can cause blinks) */
        idle = false

        lastViewport = viewport
        val visibleTiles = visibleTilesResolver.getVisibleTiles(viewport)
        setVisibleTiles(visibleTiles)
    }

    private fun setVisibleTiles(visibleTiles: VisibleTiles) {
        /* Feed the tile processing machinery */
        visibleTilesFlow.value = visibleTiles

        lastVisible = visibleTiles
        lastVisibleCount = visibleTiles.count

        evictTiles(visibleTiles)

        renderThrottled()
    }

    /**
     * Consumes incoming visible tiles from [visibleTilesFlow] and sends [TileSpec] instances to the
     * [TileCollector].
     *
     * Leverage built-in back pressure, as this function will suspend when the tile collector is busy
     * to the point it can't handshake the [visibleTileLocationsChannel] channel.
     *
     * Using [Flow.collectLatest], we cancel any ongoing previous tile list processing. It's
     * particularly useful when the [TileCollector] is too slow, so when a new [VisibleTiles] element
     * is received from [visibleTilesFlow], no new [TileSpec] elements from the previous [VisibleTiles]
     * element are sent to the [TileCollector]. Instead, the new [VisibleTiles] element is processed
     * right away.
     */
    private suspend fun collectNewTiles() {
        visibleTilesFlow.collectLatest { visibleTiles ->
            if (visibleTiles != null) {
                for (e in visibleTiles.tileMatrix) {
                    val row = e.key
                    val colRange = e.value
                    for (col in colRange) {
                        val alreadyProcessed = tilesToRender.any { tile ->
                            tile.sameSpecAs(visibleTiles.level, row, col, visibleTiles.subSample)
                        }
                        /* Only emit specs which haven't already been processed by the collector
                         * Doing this now results in less object allocations than filtering the flow
                         * afterwards */
                        if (!alreadyProcessed) {
                            visibleTileLocationsChannel.send(TileSpec(visibleTiles.level, row, col, visibleTiles.subSample))
                        }
                    }
                }
            }
        }
    }

    /**
     * For each [Tile] received, add it to the list of tiles to render if it's visible. Otherwise,
     * add the corresponding Bitmap to the [bitmapPool], and assign a [Paint] object to this tile.
     * The TileCanvasView manages the alpha, but the view-model takes care of recycling those objects.
     */
    private suspend fun consumeTiles(tileChannel: ReceiveChannel<Tile>) {
        for (tile in tileChannel) {
            if (lastVisible.contains(tile) && !tilesToRender.contains(tile)) {
                tile.setPaint()
                tilesToRender.add(tile)
                idleDebounced.offer(Unit)
                renderThrottled()
            } else {
                tile.recycle()
            }
        }
    }

    /**
     * Pick a [Paint] from the [paintPool], or create a new one. The the alpha needs to be set to 0,
     * to produce a fade-in effect. Color filter is also set.
     */
    private fun Tile.setPaint() {
        paint = (paintPool.get() ?: Paint()).also {
            it.alpha = 0
            it.colorFilter = tileOptionsProvider.getColorFilter(row, col, zoom)
        }
    }

    private fun VisibleTiles.contains(tile: Tile): Boolean {
        val colRange = tileMatrix[tile.row] ?: return false
        return level == tile.zoom && subSample == tile.subSample && tile.col in colRange
    }

    private fun VisibleTiles.overlaps(tile: Tile): Boolean {
        val colRange = tileMatrix[tile.row] ?: return false
        return level == tile.zoom && tile.col in colRange
    }

    /**
     * Each time we get a new [VisibleTiles], remove all [Tile] from [tilesToRender] which aren't
     * visible or that aren't needed anymore and put their bitmap into the pool.
     */
    private fun evictTiles(visibleTiles: VisibleTiles) {
        val currentLevel = visibleTiles.level
        val currentSubSample = visibleTiles.subSample

        /* Always remove tiles that aren't visible at current level */
        val iterator = tilesToRender.iterator()
        while (iterator.hasNext()) {
            val tile = iterator.next()
            if (tile.zoom == currentLevel && tile.subSample == visibleTiles.subSample && !visibleTiles.contains(tile)) {
                iterator.remove()
                tile.recycle()
            }
        }

        if (!idle) {
            partialEviction(visibleTiles)
        } else {
            aggressiveEviction(currentLevel, currentSubSample)
        }
    }

    /**
     * Evict tiles for levels different than the current one, that aren't visible.
     */
    private fun partialEviction(visibleTiles: VisibleTiles) {
        val currentLevel = visibleTiles.level

        /* First, deal with tiles of other levels that aren't sub-sampled */
        val otherTilesNotSubSampled = tilesToRender.filter {
            it.zoom != currentLevel && it.subSample == 0
        }
        val evictList = mutableListOf<Tile>()
        if (otherTilesNotSubSampled.isNotEmpty()) {
            val byLevel = otherTilesNotSubSampled.groupBy { it.zoom }
            byLevel.forEach { (level, tiles) ->
                val visibleAtLevel = visibleTilesResolver.getVisibleTiles(lastViewport, level)
                tiles.filter {
                    !visibleAtLevel.overlaps(it)
                }.let {
                    evictList.addAll(it)
                }
            }
        }

        /* Then, evict sub-sampled tiles that aren't visible anymore */
        val subSampledTiles = tilesToRender.filter {
            it.subSample > 0
        }
        if (subSampledTiles.isNotEmpty()) {
            val visibleAtLowestLevel = visibleTilesResolver.getVisibleTiles(lastViewport, 0)
            subSampledTiles.filter {
                !visibleAtLowestLevel.overlaps(it)
            }.let {
                evictList.addAll(it)
            }
        }

        val iterator = tilesToRender.iterator()
        while (iterator.hasNext()) {
            val tile = iterator.next()
            evictList.any {
                it.samePositionAs(tile)
            }.let {
                if (it) {
                    iterator.remove()
                    tile.recycle()
                }
            }
        }
    }

    /**
     * Only triggered after the [idleDebounced] fires.
     */
    private fun aggressiveEviction(currentLevel: Int, currentSubSample: Int) {
        /**
         * If not all tiles at current level (or also current sub-sample) are fetched, don't go
         * further.
         */
        val nTilesAtCurrentLevel = tilesToRender.count {
            it.zoom == currentLevel && it.subSample == currentSubSample
        }
        if (nTilesAtCurrentLevel < lastVisibleCount) {
            return
        }

        val otherTilesNotSubSampled = tilesToRender.filter {
            it.zoom != currentLevel
        }

        val subSampledTiles = tilesToRender.filter {
            it.zoom == 0 && it.subSample != currentSubSample
        }

        val iterator = tilesToRender.iterator()
        while (iterator.hasNext()) {
            val tile = iterator.next()
            val found = otherTilesNotSubSampled.any {
                it.samePositionAs(tile)
            }
            if (found) {
                iterator.remove()
                tile.recycle()
                continue
            }

            if (subSampledTiles.contains(tile)) {
                iterator.remove()
                tile.recycle()
            }
        }
    }

    /**
     * Post a new value to the observable. The view should update its UI.
     */
    private fun renderThrottled() {
        renderTask.offer(Unit)
    }

    /**
     * After a [Tile] is no longer visible, recycle its Bitmap and Paint if possible, for later use.
     */
    private fun Tile.recycle() {
        if (bitmap.isMutable) {
            bitmapPool.put(bitmap)
        }
        paint?.let {
            paint = null
            it.alpha = 0
            it.colorFilter = null
            paintPool.put(it)
        }
    }
}

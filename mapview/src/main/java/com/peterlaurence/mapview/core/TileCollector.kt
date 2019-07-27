package com.peterlaurence.mapview.core

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.selects.select


/**
 * The engine of the MapView. The view-model uses two channels to communicate with the [TileCollector]:
 * * one to send [TileSpec]s (a [SendChannel])
 * * one to receive [Tile]s (a [ReceiveChannel])
 *
 * The [TileCollector] encapsulates all the complexity that transforms a [TileSpec] into a [Tile].
 * ```
 *                                              _____________________________________________________________________
 *                                             |                           TileCollector             ____________    |
 *                                  tiles      |                                                    |  ________  |   |
 *              ---------------- [*********] <----------------------------------------------------- | | worker | |   |
 *             |                               |                                                    |  --------  |   |
 *             ↓                               |                                                    |  ________  |   |
 *  _____________________                      |                                  tileStatus        | | worker | |   |
 * | TileCanvasViewModel |                     |    _____________________  <---- [**********] <---- |  --------  |   |
 *  ---------------------  ----> [*********] ----> | tileCollectorKernel |                          |  ________  |   |
 *                                tileSpecs    |    ---------------------  ----> [**********] ----> | | worker | |   |
 *                                             |                                  tileStatus        |  --------  |   |
 *                                             |                                                    |____________|   |
 *                                             |                                                      worker pool    |
 *                                             |                                                                     |
 *                                              ---------------------------------------------------------------------
 * ```
 *
 * @author peterLaurence on 22/06/19
 */
class TileCollector(private val workerCount: Int) {

    /**
     * Sets up the tile collector machinery. The architecture is inspired from
     * [Kotlin Conf 2018](https://www.youtube.com/watch?v=a3agLJQ6vt8).
     * @param [tileSpecs] channel of [TileSpec], which capacity should be [Channel.CONFLATED].
     * @param [tilesOutput] channel of [Tile], which should be set as [Channel.RENDEZVOUS].
     */
    fun CoroutineScope.collectTiles(tileSpecs: ReceiveChannel<List<TileSpec>>,
                                    tilesOutput: SendChannel<Tile>,
                                    tileStreamProvider: TileStreamProvider,
                                    bitmapFlow: Flow<Bitmap>) {
        val tilesToDownload = Channel<TileStatus>(capacity = Channel.RENDEZVOUS)
        val tilesDownloadedFromWorker = Channel<TileStatus>(capacity = Channel.UNLIMITED)

        repeat(workerCount) { worker(tilesToDownload, tilesDownloadedFromWorker, tilesOutput, tileStreamProvider, bitmapFlow) }
        tileCollectorKernel(tileSpecs, tilesToDownload, tilesDownloadedFromWorker)
    }

    private fun CoroutineScope.worker(tilesToDownload: ReceiveChannel<TileStatus>,
                                      tilesDownloaded: SendChannel<TileStatus>,
                                      tilesOutput: SendChannel<Tile>,
                                      tileStreamProvider: TileStreamProvider,
                                      bitmapFlow: Flow<Bitmap>) = launch(Dispatchers.IO) {

        val bitmapLoadingOptions = BitmapFactory.Options()
        bitmapLoadingOptions.inPreferredConfig = Bitmap.Config.RGB_565

        for (tileStatus in tilesToDownload) {
            /* If it was cancelled, do nothing and send back the TileSpec as is */
            if (tileStatus.cancelled) {
                tilesDownloaded.send(tileStatus)
                continue
            }

            val spec = tileStatus.spec

            val i = tileStreamProvider.getTileStream(spec.row, spec.col, spec.zoom)

            if (spec.subSample > 0) {
                bitmapLoadingOptions.inBitmap = null
                bitmapLoadingOptions.inScaled = true
                bitmapLoadingOptions.inSampleSize = spec.subSample
            } else {
                bitmapLoadingOptions.inScaled = false
                bitmapLoadingOptions.inBitmap = bitmapFlow.single()
                bitmapLoadingOptions.inSampleSize = 0
            }

            try {
                val bitmap = BitmapFactory.decodeStream(i, null, bitmapLoadingOptions) ?: continue
                val tile = Tile(spec.zoom, spec.row, spec.col, spec.subSample).apply {
                    this.bitmap = bitmap
                }
                tilesOutput.send(tile)
            } catch (e: OutOfMemoryError) {
                // no luck
            } catch (e: Exception) {
                // maybe retry
            } finally {
                tilesDownloaded.send(tileStatus)
                i?.close()
            }
        }
    }

    private fun CoroutineScope.tileCollectorKernel(tileSpecs: ReceiveChannel<List<TileSpec>>,
                                                   tilesToDownload: SendChannel<TileStatus>,
                                                   tilesDownloadedFromWorker: ReceiveChannel<TileStatus>) = launch(Dispatchers.Default) {

        val tilesBeingProcessed = mutableListOf<TileStatus>()

        while (true) {
            select<Unit> {
                tileSpecs.onReceive {
                    for (loc in it) {
                        if (!tilesBeingProcessed.any { status -> status.spec == loc }) {
                            /* Add it to the list of locations being processed */
                            val status = TileStatus(loc)
                            tilesBeingProcessed.add(status)

                            /* Now download the tile */
                            tilesToDownload.send(status)
                        }
                    }
                    for (status in tilesBeingProcessed) {
                        if (!it.contains(status.spec)) {
                            status.cancelled = true
                        }
                    }
                }

                tilesDownloadedFromWorker.onReceive {
                    tilesBeingProcessed.remove(it)
                }
            }
        }
    }

    data class TileStatus(val spec: TileSpec, @Volatile var cancelled: Boolean = false)
}
package com.peterlaurence.mapview.demo.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.MapViewConfiguration
import com.peterlaurence.mapview.core.TileStreamProvider
import com.peterlaurence.mapview.demo.R
import java.io.BufferedInputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * This example is based on [MapAloneFragment], except that the [TileStreamProvider] fetches remote
 * HTTP tiles. The distant server used in this example is a simple VPS with low performance.
 */
class RemoteHttpFragment : Fragment() {
    private lateinit var parentView: ViewGroup

    /**
     * The [MapView] should always be added inside [onCreateView], to ensure state save/restore.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_layout, container, false).also {
            parentView = it as ViewGroup
            context?.let { ctx ->
                makeMapView(ctx)?.addToFragment()
            }
        }
    }

    /**
     * Assign an id the the [MapView] (it's necessary to enable state save/restore).
     */
    private fun MapView.addToFragment() {
        id = R.id.mapview_id
        isSaveEnabled = true

        parentView.addView(this, 0)
    }

    /**
     * In this example, the configuration is done **immediately** after the [MapView] is added to
     * the view hierarchy, in [onCreateView].
     * But it's not mandatory, it could have been done later on. But beware to configure only once.
     */
    private fun makeMapView(context: Context): MapView? {
        val tileStreamProvider = object : TileStreamProvider {
            override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
                return try {
                    val url = URL("https://plrapps.ovh:8080/mapview-tile/$zoomLvl/$row/$col.jpg")
                    val connection = createConnection(url)
                    connection.connect()
                    BufferedInputStream(connection.inputStream)
                } catch (e: Exception) {
                    null
                }
            }

            fun createConnection(url: URL): HttpURLConnection {
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                return connection
            }
        }
        val tileSize = 256

        /**
         * Very important: for remote tiles, we have to raise the number of workers to be more
         * efficient. Don't put a value greater than 60.
         * Keep in mind that more workers will put more load onto the device, which can lead to
         * stutters. It's a balance between device capability, distant server response time, and
         * fluid experience.
         */
        val config = MapViewConfiguration(
                5, 8192, 8192, tileSize, tileStreamProvider
        ).setMaxScale(2f).setPadding(tileSize * 2).setWorkerCount(16)

        return MapView(context).apply {
            configure(config)
        }
    }
}
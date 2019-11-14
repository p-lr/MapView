package com.peterlaurence.mapview.demo.fragments

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
    private lateinit var mapView: MapView
    private lateinit var parentView: ViewGroup

    /**
     * The [MapView] should always be added inside [onCreateView], to ensure state save/restore.
     */
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_layout, container, false).also {
            parentView = it as ViewGroup
            context?.let { ctx ->
                MapView(ctx).addToFragment()
            }
        }
    }

    /**
     * Assign an id the the [MapView] (it's necessary to enable state save/restore).
     * And keep a ref on the [MapView]
     */
    private fun MapView.addToFragment() {
        this@RemoteHttpFragment.mapView = this

        mapView.id = R.id.mapview_id
        mapView.isSaveEnabled = true

        parentView.addView(mapView, 0)
    }

    /**
     * In this example, the configuration isn't done **immediately** after the [MapView] is added to
     * the view hierarchy, in [onCreateView]. It's done in [onStart].
     * But it's not mandatory, it could have been done right after the [MapView] creation.
     */
    override fun onStart() {
        super.onStart()

        val tileStreamProvider = object : TileStreamProvider {
            override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
                return try {
                    val url = URL("http://plrapps.ovh:8080/mapview-tile/$zoomLvl/$row/$col.jpg")
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

        mapView.configure(config)
    }
}
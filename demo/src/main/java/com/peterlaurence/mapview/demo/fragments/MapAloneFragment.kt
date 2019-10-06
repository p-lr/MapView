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
import java.io.InputStream

/**
 * An example showing the simplest usage of [MapView].
 */
class MapAloneFragment : Fragment() {
    private lateinit var mapView: MapView
    private lateinit var parentView: ViewGroup

    /**
     * The [MapView] should always be added inside [onCreateView], to ensure state save/restore.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        this@MapAloneFragment.mapView = this

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
                    context?.assets?.open("tiles/esp/$zoomLvl/$row/$col.jpg")
                } catch (e: Exception) {
                    null
                }
            }
        }
        val tileSize = 256
        val config = MapViewConfiguration(
            5, 8192, 8192, tileSize, tileStreamProvider
        ).setMaxScale(2f).setPadding(tileSize * 2)

        mapView.configure(config)
    }
}
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

class MapAloneFragment : Fragment() {
    private lateinit var mapView: MapView
    private lateinit var parentView: ViewGroup

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_layout, container, false).also {
            parentView = it as ViewGroup
            makeMapView()?.addToFragment()
        }
    }

    private fun makeMapView(): MapView? {
        val context = context ?: return null
        val mapView = MapView(context)

        val tileStreamProvider = object : TileStreamProvider {
            override fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream? {
                return try {
                    context.assets.open("tiles/esp/$zoomLvl/$row/$col.jpg")
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
        return mapView
    }

    private fun MapView.addToFragment() {
        this@MapAloneFragment.mapView = this

        /* This is necessary to ensure state save/restore */
        mapView.id = R.id.mapview_id
        mapView.isSaveEnabled = true

        parentView.addView(mapView, 0)
    }
}
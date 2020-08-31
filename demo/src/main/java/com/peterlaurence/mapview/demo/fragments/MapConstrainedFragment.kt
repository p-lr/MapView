package com.peterlaurence.mapview.demo.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.MapViewConfiguration
import com.peterlaurence.mapview.api.constrainScroll
import com.peterlaurence.mapview.core.TileStreamProvider
import com.peterlaurence.mapview.demo.R
import kotlin.random.Random

/**
 * An example showing how to constrain [MapView] to only display a specific area, using
 * [MapView.constrainScroll].
 */
class MapConstrainedFragment : Fragment() {
    private lateinit var parentView: ViewGroup
    private lateinit var button: Button

    /**
     * The [MapView] should always be added inside [onCreateView], to ensure state save/restore.
     */
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_btn_layout, container, false).also {
            parentView = it as ViewGroup
            button = it.findViewById(R.id.button)
            button.text = getString(R.string.change_area)
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
        val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
            try {
                context.assets?.open("tiles/esp/$zoomLvl/$row/$col.jpg")
            } catch (e: Exception) {
                null
            }
        }
        val tileSize = 256
        val config = MapViewConfiguration(
                5, 8192, 8192, tileSize, tileStreamProvider
        ).setMaxScale(2f)

        return MapView(context).apply {
            configure(config)
            defineBounds(0.0, 0.0, 1.0, 1.0)

            /* Constrain to the area:
             * ________________________
             * |           ↑          |
             * |           |50%       |
             * |   50%   __↓_____ 30% |
             * |<------->|      |<--->|
             * |         |      |     |
             * |         -------      |
             * |           ↕30%       |
             * -----------------------
             */
            constrainScroll(0.5, 0.5, 0.7, 0.7)

            /* Check the behavior on dynamic area swap */
            button.setOnClickListener {
                val relativeMinX = Random.nextDouble()
                val relativeMaxX = (relativeMinX + 0.5).coerceAtMost(1.0)
                val relativeMinY = Random.nextDouble()
                val relativeMaxY = (relativeMinY + 0.3).coerceAtMost(1.0)

                constrainScroll(relativeMinX, relativeMinY, relativeMaxX, relativeMaxY)
            }
        }
    }
}
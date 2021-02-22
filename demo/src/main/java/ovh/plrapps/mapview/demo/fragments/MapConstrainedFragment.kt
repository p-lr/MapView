package ovh.plrapps.mapview.demo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.MapViewConfiguration
import ovh.plrapps.mapview.api.constrainScroll
import ovh.plrapps.mapview.core.TileStreamProvider
import ovh.plrapps.mapview.demo.R
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
        return inflater.inflate(R.layout.fragment_map_constrained, container, false).also {
            parentView = it as ViewGroup
            button = it.findViewById(R.id.button)
            button.text = getString(R.string.change_area)
            configureMapView(it)
        }
    }

    /**
     * In this example, the configuration is done **immediately** after the [MapView] is added to
     * the view hierarchy, in [onCreateView].
     * But it's not mandatory, it could have been done later on. However, beware to configure only once.
     */
    private fun configureMapView(view: View) {
        val mapView = view.findViewById<MapView>(R.id.mapview) ?: return
        val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
            try {
                mapView.context.assets?.open("tiles/esp/$zoomLvl/$row/$col.jpg")
            } catch (e: Exception) {
                null
            }
        }
        val tileSize = 256
        val config = MapViewConfiguration(
                5, 8192, 8192, tileSize, tileStreamProvider
        ).setMaxScale(2f)

        mapView.apply {
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
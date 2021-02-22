package ovh.plrapps.mapview.demo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.MapViewConfiguration
import ovh.plrapps.mapview.core.TileStreamProvider
import ovh.plrapps.mapview.demo.R

/**
 * An example showing the simplest usage of [MapView].
 */
class MapAloneFragment : Fragment() {
    private lateinit var parentView: ViewGroup

    /**
     * The [MapView] should always be added inside [onCreateView], to ensure state save/restore.
     */
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment, container, false).also {
            parentView = it as ViewGroup
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
                view.context.assets?.open("tiles/esp/$zoomLvl/$row/$col.jpg")
            } catch (e: Exception) {
                null
            }
        }
        val tileSize = 256
        val config = MapViewConfiguration(
                5, 8192, 8192, tileSize, tileStreamProvider
        ).setMaxScale(2f)

        mapView.configure(config)
    }
}
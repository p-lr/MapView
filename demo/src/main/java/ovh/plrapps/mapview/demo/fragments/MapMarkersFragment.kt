package ovh.plrapps.mapview.demo.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.MapViewConfiguration
import ovh.plrapps.mapview.api.addCallout
import ovh.plrapps.mapview.api.addMarker
import ovh.plrapps.mapview.api.removeMarker
import ovh.plrapps.mapview.api.setMarkerTapListener
import ovh.plrapps.mapview.core.TileStreamProvider
import ovh.plrapps.mapview.demo.R
import ovh.plrapps.mapview.demo.fragments.views.MapMarker
import ovh.plrapps.mapview.demo.fragments.views.MarkerCallout
import ovh.plrapps.mapview.markers.MarkerTapListener

/**
 * An example showing the usage of markers.
 */
class MapMarkersFragment : Fragment() {
    private lateinit var parentView: ViewGroup

    /**
     * The [MapView] should always be added inside [onCreateView], to ensure state save/restore.
     * In this example, the configuration is done **immediately** after the [MapView] is added to
     * the view hierarchy.
     * But it's not mandatory, it could have been done later on.
     */
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_markers, container, false).also {
            parentView = it as ViewGroup
            configureMapView(it)
        }
    }

    private fun configureMapView(view: View) {
        val mapView = view.findViewById<MapView>(R.id.mapview) ?: return

        val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
            try {
                mapView.context.assets.open("tiles/esp/$zoomLvl/$row/$col.jpg")
            } catch (e: Exception) {
                null
            }
        }
        val tileSize = 256
        val config = MapViewConfiguration(
                5, 8192, 8192, tileSize, tileStreamProvider
        ).setMaxScale(2f).setPadding(tileSize * 2)

        mapView.configure(config)

        mapView.defineBounds(0.0, 0.0, 1.0, 1.0)

        addNewMarker(mapView, 0.5, 0.5, "marker #1")
        addNewMarker(mapView, 0.4, 0.3, "marker #2")

        var specialMarker = addSpecialMarker(mapView)


        /* When a marker is tapped, we want to show a callout view */
        mapView.setMarkerTapListener(object : MarkerTapListener {
            override fun onMarkerTap(view: View, x: Int, y: Int) {
                if (view is MapMarker) {
                    val callout = MarkerCallout(mapView.context)
                    callout.setTitle(view.name)
                    callout.setSubTitle("position: ${view.x} , ${view.y}")
                    mapView.addCallout(callout, view.x, view.y, -0.5f, -1.2f, 0f, 0f)
                    callout.transitionIn()
                }
            }
        })

        /* Below is the configuration of the button to add/remove the special marker */
        val button = view.findViewById<AppCompatButton>(R.id.add_remove_button)
        button.text = REMOVE_MARKER
        button.setOnClickListener(object : View.OnClickListener {
            var added = true
            override fun onClick(v: View?) {
                added = !added
                if (!added) {
                    mapView.removeMarker(specialMarker)
                } else {
                    specialMarker = addSpecialMarker(mapView)
                }
                button.text = if (added) REMOVE_MARKER else ADD_MARKER
            }
        })
    }

    private fun addNewMarker(mapView: MapView, x: Double, y: Double, name: String) {
        val marker = MapMarker(requireContext(), x, y, name).apply {
            setImageResource(R.drawable.map_marker)
        }

        mapView.addMarker(marker, x, y)
    }

    private fun addSpecialMarker(mapView: MapView): MapMarker {
        val x = 0.6
        val y = 0.4
        val marker = MapMarker(requireContext(), x, y, "special marker").apply {
            setColorFilter(ContextCompat.getColor(this.context, R.color.colorAccent))
            setImageResource(R.drawable.map_marker_circle)
        }

        /* Since the marker is circular, we want to center it on the position. So we use 0.5f as
         * relative anchors */
        mapView.addMarker(marker, x, y, relativeAnchorLeft = -0.5f, relativeAnchorTop = -0.5f)
        return marker
    }
}

private const val ADD_MARKER = "Add marker"
private const val REMOVE_MARKER = "Remove marker"


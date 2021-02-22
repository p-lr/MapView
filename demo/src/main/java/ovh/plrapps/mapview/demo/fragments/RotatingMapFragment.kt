package ovh.plrapps.mapview.demo.fragments

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.MapViewConfiguration
import ovh.plrapps.mapview.ReferentialData
import ovh.plrapps.mapview.ReferentialListener
import ovh.plrapps.mapview.api.addCallout
import ovh.plrapps.mapview.api.addMarker
import ovh.plrapps.mapview.api.setMarkerTapListener
import ovh.plrapps.mapview.core.TileStreamProvider
import ovh.plrapps.mapview.demo.R
import ovh.plrapps.mapview.demo.fragments.views.MapMarker
import ovh.plrapps.mapview.demo.fragments.views.MarkerCallout
import ovh.plrapps.mapview.markers.MarkerTapListener
import ovh.plrapps.mapview.paths.PathPoint
import ovh.plrapps.mapview.paths.PathView
import ovh.plrapps.mapview.paths.addPathView
import ovh.plrapps.mapview.paths.toFloatArray
import ovh.plrapps.mapview.util.AngleDegree

/**
 * An example showing the usage of a rotating map. It features markers and paths.
 */
class RotatingMapFragment : Fragment() {
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
        return inflater.inflate(R.layout.fragment, container, false).also {
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
        ).setMaxScale(2f).enableRotation()

        mapView.configure(config)

        mapView.defineBounds(0.0, 0.0, 1.0, 1.0)

        mapView.addNewMarker(0.5, 0.5, "marker #1")
        mapView.addNewMarker(0.4, 0.3, "marker #2")
        mapView.addNewMarker(0.6, 0.4, "marker #3")

        val positionMarker = mapView.addPositionMarker(0.7, 0.7)

        /* A referential owner holds a ReferentialData and is notified by the MapView of referential
         * changes.
         * Here, we want to rotate the positionMarker so that it turns along with the map. */
        val refOwner = object : ReferentialListener {
            var referentialData: ReferentialData? = null

            override fun onReferentialChanged(refData: ReferentialData) {
                referentialData = refData
                rotateMaker()
            }

            /* Add an offset, which we'll change dynamically by taping the marker */
            var angleDegree: AngleDegree = 0f
                set(value) {
                    field = value
                    rotateMaker()
                }

            private fun rotateMaker() {
                val refData = referentialData ?: return
                positionMarker.rotation = angleDegree + refData.angle
            }
        }

        /* Register the ReferentialListener */
        mapView.addReferentialListener(refOwner)

        /* When a marker is tapped, we want to show a callout view */
        mapView.setMarkerTapListener(object : MarkerTapListener {
            override fun onMarkerTap(view: View, x: Int, y: Int) {
                if (view is MapMarker) {
                    if (view.name == POSITION_MARKER) {
                        /* Change the angle offset */
                        val randomAngle = (0..360).random().toFloat()
                        refOwner.angleDegree = randomAngle
                    } else {
                        val callout = MarkerCallout(mapView.context)
                        callout.setTitle(view.name)
                        callout.setSubTitle("position: ${view.x} , ${view.y}")
                        mapView.addCallout(callout, view.x, view.y, -0.5f, -1.2f, 0f, 0f)
                        callout.transitionIn()
                    }
                }
            }
        })

        val pathView = PathView(mapView.context)
        mapView.addPathView(pathView)

        val pathList = listOfNotNull(
                listOf(
                        PathPoint(0.2, 0.3), PathPoint(0.25, 0.15), PathPoint(0.32, 0.1),
                        PathPoint(0.427, 0.212), PathPoint(0.6, 0.15), PathPoint(0.67, 0.1)
                ).toFloatArray(mapView),
                listOf(
                        PathPoint(0.5, 0.5), PathPoint(0.55, 0.52), PathPoint(0.57, 0.54),
                        PathPoint(0.59, 0.52), PathPoint(0.6, 0.51), PathPoint(0.59, 0.5),
                        PathPoint(0.578, 0.447), PathPoint(0.46, 0.44), PathPoint(0.5, 0.5)
                ).toFloatArray(mapView)
        ).map {
            object : PathView.DrawablePath {
                override val visible: Boolean = true
                override var path: FloatArray = it
                override var paint: Paint? = null
                override val width: Float? = null
            }
        }

        pathView.updatePaths(pathList)
    }

    private fun MapView.addNewMarker(x: Double, y: Double, name: String) {
        val marker = MapMarker(context, x, y, name).apply {
            setImageResource(R.drawable.map_marker)
        }

        addMarker(marker, x, y)
    }

    private fun MapView.addPositionMarker(x: Double, y: Double): View {
        val marker = MapMarker(context, x, y, POSITION_MARKER).apply {
            setImageResource(R.drawable.position_marker)
        }

        addMarker(marker, x, y, -0.5f, -0.5f)
        return marker
    }
}

const val POSITION_MARKER = "position marker"
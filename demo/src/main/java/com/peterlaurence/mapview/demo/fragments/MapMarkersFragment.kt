package com.peterlaurence.mapview.demo.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.*
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import com.peterlaurence.mapview.MapView
import com.peterlaurence.mapview.MapViewConfiguration
import com.peterlaurence.mapview.core.TileStreamProvider
import com.peterlaurence.mapview.demo.R
import com.peterlaurence.mapview.markers.MarkerTapListener
import com.peterlaurence.mapview.markers.addCallout
import com.peterlaurence.mapview.markers.addMarker
import com.peterlaurence.mapview.markers.setMarkerTapListener
import java.io.InputStream

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

        mapView.defineBounds(0.0, 0.0, 1.0, 1.0)

        mapView.addNewMarker(0.5, 0.5, "marker #1")
        mapView.addNewMarker(0.4, 0.3, "marker #2")
        mapView.addNewMarker(0.6, 0.4, "marker #3")


        /* When a marker is tapped, we want to show a callout view */
        mapView.setMarkerTapListener(object : MarkerTapListener {
            override fun onMarkerTap(view: View, x: Int, y: Int) {
                if (view is MapMarker) {
                    val callout = MarkerCallout(context)
                    callout.setTitle(view.name)
                    callout.setSubTitle("position: ${view.x} , ${view.y}")
                    mapView.addCallout(callout, view.x, view.y, -0.5f, -1.2f, 0f, 0f)
                    callout.transitionIn()
                }
            }
        })
        return mapView
    }

    /**
     * Assign an id the the [MapView] (it's necessary to enable state save/restore).
     */
    private fun MapView.addToFragment() = apply {
        id = R.id.mapview_id
        isSaveEnabled = true

        parentView.addView(this, 0)
    }

    private fun MapView.addNewMarker(x: Double, y: Double, name: String) {
        val marker = MapMarker(context, x, y, name).apply {
            setImageResource(R.drawable.map_marker)
        }

        addMarker(marker, x, y)
    }
}

/**
 * A custom [View] that serves as map marker. It bundles its own position and name.
 */
class MapMarker(context: Context, val x: Double, val y: Double, val name: String) :
    AppCompatImageView(context)

/**
 * A view that will pop-pup when a marker is tapped.
 */
class MarkerCallout(context: Context) : RelativeLayout(context) {
    private val mTitle: TextView
    private val mSubTitle: TextView

    init {

        View.inflate(context, R.layout.marker_callout, this)

        mTitle = findViewById(R.id.callout_title)
        mSubTitle = findViewById(R.id.callout_subtitle)
    }

    fun transitionIn() {
        val scaleAnimation =
            ScaleAnimation(
                0f,
                1f,
                0f,
                1f,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                1f
            )
        scaleAnimation.interpolator = OvershootInterpolator(1.2f)
        scaleAnimation.duration = 250

        val alphaAnimation = AlphaAnimation(0f, 1f)
        alphaAnimation.duration = 200

        val animationSet = AnimationSet(false)

        animationSet.addAnimation(scaleAnimation)
        animationSet.addAnimation(alphaAnimation)

        startAnimation(animationSet)
    }

    fun setTitle(title: String) {
        mTitle.text = title
    }

    fun setSubTitle(subtitle: String) {
        mSubTitle.text = subtitle
    }
}

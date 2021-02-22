package ovh.plrapps.mapview.demo.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.MapViewConfiguration
import ovh.plrapps.mapview.core.TileStreamProvider
import ovh.plrapps.mapview.demo.R
import kotlinx.coroutines.delay

/**
 * An example showing deferred configuration of [MapView]. In this example, a [MapView] is first
 * added to the view hierarchy, then a few moment later it's configured.
 *
 * It demonstrates that the [MapView] configuration can be done later (when all necessary data is
 * gathered). It's important since the Android framework requires a view to be added inside the
 * [onCreateView] lifecycle callback to have its state saved and restored upon device rotation.
 * But inside [onCreateView] we may not have (yet) all information needed to call [MapView.configure].
 */
class DeferredFragment : Fragment() {

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
        return inflater.inflate(R.layout.fragment_deferred, container, false).also {
            parentView = it as ViewGroup
            context?.let { ctx ->
                MapView(ctx).addToFragment()
            }
            deferredConfigure()
        }
    }

    /**
     * Assign an id the the [MapView] (it's necessary to enable state save/restore).
     * And keep a ref on the [MapView]
     */
    private fun MapView.addToFragment() {
        this@DeferredFragment.mapView = this

        mapView.id = R.id.mapview_id
        mapView.isSaveEnabled = true

        parentView.addView(mapView, 0)
    }

    /**
     * In this example, the configuration isn't done **immediately** after the [MapView] is added to
     * the view hierarchy, in [onCreateView]. It's done after a delay.
     * Beware to configure the [MapView] only once. Or, call [MapView.destroy] on the existing
     * instance then create and configure a new instance.
     */
    private fun deferredConfigure() = lifecycleScope.launchWhenStarted {
        // simulate delay
        delay(500)

        val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
            try {
                context?.assets?.open("tiles/esp/$zoomLvl/$row/$col.jpg")
            } catch (e: Exception) {
                null
            }
        }
        val tileSize = 256
        val config = MapViewConfiguration(
                5, 8192, 8192, tileSize, tileStreamProvider
        ).setMaxScale(2f).setPadding(tileSize * 2)

        mapView.configure(config)
    }
}
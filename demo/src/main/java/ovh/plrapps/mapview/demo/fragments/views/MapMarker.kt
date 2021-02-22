package ovh.plrapps.mapview.demo.fragments.views

import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

/**
 * A custom [View] that serves as map marker. It bundles its own position and name.
 */
class MapMarker(context: Context, val x: Double, val y: Double, val name: String) :
        AppCompatImageView(context)
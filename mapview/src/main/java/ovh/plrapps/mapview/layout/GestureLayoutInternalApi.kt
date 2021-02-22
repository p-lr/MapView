package ovh.plrapps.mapview.layout

import ovh.plrapps.mapview.core.InternalMapViewApi

@InternalMapViewApi
internal fun GestureLayout.setSize(width: Int, height: Int) {
    gestureController.setSize(width, height)
}


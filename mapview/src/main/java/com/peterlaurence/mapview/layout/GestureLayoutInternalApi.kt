package com.peterlaurence.mapview.layout

import com.peterlaurence.mapview.core.InternalMapViewApi

@InternalMapViewApi
internal fun GestureLayout.setSize(width: Int, height: Int) {
    scaleController.setSize(width, height)
}


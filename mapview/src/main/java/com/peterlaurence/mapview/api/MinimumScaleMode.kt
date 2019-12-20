package com.peterlaurence.mapview.api

enum class MinimumScaleMode {
    /**
     * Limit the minimum scale to no less than what
     * would be required to fill the container
     */
    FILL,

    /**
     * Limit the minimum scale to no less than what
     * would be required to fit inside the container
     */
    FIT,

    /**
     * Allow arbitrary minimum scale.
     */
    NONE
}
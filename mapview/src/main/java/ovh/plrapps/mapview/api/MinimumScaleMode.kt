package ovh.plrapps.mapview.api

enum class MinimumScaleMode {
    /**
     * Limit the minimum scale to no less than what would be required to fill the container.
     */
    FILL,

    /**
     * Limit the minimum scale to no less than what would be required to fit inside the container.
     * This is the default mode.
     */
    FIT,

    /**
     * Allow arbitrary positive minimum scale.
     */
    NONE
}
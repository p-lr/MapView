# Animate rotation

You can use your own animator, like so:
```kotlin
private fun animateMapViewToNorth() {
    /* Wrapper class, necessary for the the animator to work (which uses reflection to infer
     * method names..) */
     @Suppress("unused")
     val wrapper = object {
         fun setAngle(angle: Float) {
             mapView?.setAngle(angle)
         }

         fun getAngle(): Float {
             return referentialData.angle
         }
     }
     ObjectAnimator.ofFloat(wrapper, "angle", if (referentialData.angle > 180f) 360f else 0f).apply {
         interpolator = DecelerateInterpolator()
         duration = 800
         start()
     }
}
```
Here, we assume that `animateMapViewToNorth` is part of a class which implements the `ReferentialOwner`
interface.

## Disable fade-in effect for tiles

Starting from `MapView` v.2.0.6, you can set a `TileOptionsProvider` to the `MapView`.

```kotlin
interface TileOptionsProvider {
    /* Must not be a blocking call - should return immediately */
    @JvmDefault
    fun getColorFilter(row: Int, col: Int, zoomLvl: Int): ColorFilter? = null

    /**
     * Controls the speed of fade in effect when rendering tiles. Higher values make alpha
     * value go to 255 faster. Should be in between (0.0f, 1.0f].
     */
    @JvmDefault
    val alphaTick : Float
        get() = 0.07f
}
```

To disable fade-in effect, you can configure your `MapView` like so:
```kotlin
val tileOptionsProvider = object : TileOptionsProvider {
      override val alphaTick: Float
           get() = 255f
}

val config = MapViewConfiguration(
    5, 8192, 8192, tileSize, tileStreamProvider)
.setMaxScale(2f).setTileOptionsProvider(tileOptionsProvider)
```

You might have to add this to your android{} section of your build.gradle:
```groovy
compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
kotlinOptions {
    jvmTarget = JavaVersion.VERSION_1_8
    freeCompilerArgs = ['-Xjvm-default=compatibility']
}
```
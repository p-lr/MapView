[ ![Download](https://api.bintray.com/packages/peterlaurence/maven/mapview/images/download.svg?version=2.1.1) ](https://bintray.com/peterlaurence/maven/mapview/2.1.1/link)

# MapView

MapView is a Fast, memory efficient Android library to display tiled maps with minimal effort.

  ![output0](https://user-images.githubusercontent.com/15638794/85230007-8d738280-b3ed-11ea-8493-9965a8982294.gif)
  ![output1](https://user-images.githubusercontent.com/15638794/85230084-fc50db80-b3ed-11ea-8ec0-9f03a14ffba4.gif)

An example of setting up:

```kotlin
val mapView = MapView(context)
val tileStreamProvider = TileStreamProvider { row, col, zoomLvl ->
    FileInputStream(File("path/{zoomLvl}/{row}/{col}.jpg")) // or it can be a remote HTTP fetch
}

val config = MapViewConfiguration(levelCount = 7, fullWidth = 25000, fullHeight = 12500,
                                  tileSize = 256, tileStreamProvider = tileStreamProvider)
                                  .setMaxScale(2f)

/* Configuration */
mapView.configure(config)
```

MapView shows only the visible part of a tiled map, and supports flinging, dragging, scaling, and 
rotating. It's also possible to add markers and paths.

This project holds the source code of this library, plus a demo app (which is useful to get started).
To test the demo, just clone the repo and launch the demo app from Android Studio.

## MapView 2.x.x is out!

This new major version brings performance improvements and a brand new feature: _map rotation_. To be 
consistent with previous versions, this is disabled by default.
To enable it, use `MapViewConfiguration.enableRotation()`. You will find a code example inside the demo
[RotatingMapFragment](demo/src/main/java/com/peterlaurence/mapview/demo/fragments/RotatingMapFragment.kt).

<p align="center">
  <img src="https://user-images.githubusercontent.com/15638794/85233196-d420a700-b404-11ea-8193-2fd98ed340b3.gif">
</p>

When enabling rotation, the `MapView` handles rotation gestures by default. If you only want to rotate
the map through APIs, then you should use `enableRotation(handleRotationGesture = false)`. The `MapView`
has a new API `setAngle`:

```kotlin
/**
 * Programmatically set the rotation angle of the MapView, in decimal degrees.
 * It should be called after the [MapView] configuration and after the [MapView] has been laid out.
 * Attempts to set the angle before [MapView] has been laid out will be ignored.
 */
fun MapView.setAngle(angle: AngleDegree)
```

**Migrating from 1.x.x**

There are some breaking changes, although most of them are just package refactoring. The interface 
`ScaleChangeListener` has been removed. If you relied on this, have a look at `ReferentialOwner`interface. 
There's an example of usage inside the `RotatingMapFragment` demo.

## Installation

Add this to your module's build.gradle
```groovy
implementation 'com.peterlaurence:mapview:2.1.1'
```

Also, for each module that uses MapView, update the module's build.gradle file, as shown below:

```groovy
android {
  ...
  // Configure only for each module that uses Java 8
  // language features (either in its source code or
  // through dependencies).
  compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
  // For Kotlin projects
  kotlinOptions {
    jvmTarget = "1.8"
  }
}
```

## Origin and motivation

As a long time contributor to [TileView](https://github.com/moagrius/TileView), I wanted to see the 
performance we would get using idiomatic Kotlin (coroutines, flows). The result was beyond my 
expectations. The overall design can be seen 
[here](https://github.com/peterLaurence/MapView/wiki/TileCollector-design).
The focus has been on efficiency (No thread contention thanks to non-blocking algorithm. Therefore,
the load on the main thread is low enough to get high fps).

Thanks for Mike (@moagrius), as this library wouldn't exist without his first contributions.

## Principles

### Deep-zoom map

MapView is optimized to display maps that have several levels, like this:

<p align="center">
<img src="doc/readme-files/deepzoom.png">
</p>

Each next level is twice bigger than the former, and provides more details. Overall, this looks like
 a pyramid. Another common name is "deep-zoom" map.
This library comes with a demo app made of a set of various use-cases such as using markers, 
paths, rotating the map, etc. All examples use the same map stored in the assets. If you wonder what
a deep-zoom maps looks like, you have a great example there.

MapView can also be used with single level maps.

### Usage

To use the MapView, you have to follow these steps:
1. Create a MapView instance
```kotlin
val mapView = MapView(context)
``` 
2. Create a `TileStreamProvider`. See [below](#TOC-TileStreamProvider) for the details.
3. Create a `MapViewConfiguration`. See [below](#TOC-MapViewConfiguration) for the details.
4. Apply the configuration
```kotlin
mapView.configure(config)
```

For more insight, you can have a look at the source of the various [demos](demo/src/main/java/com/peterlaurence/mapview/demo/fragments).

### Convention

MapView uses the convention that the last level is at scale 1. So all levels have scales between 0 and 1.
Even though you don't have to be aware of the details, it's important to know that. For example, if 
you set the max scale to 2, it means that the last level will be allowed to be upscaled to twice its
 original size (since the last level is at scale 1).
This convention allows for a simple configuration.

## Technical documentation

This section explains in details the configuration. But once configured, you can do a lot of things 
with your `MapView` instance. There is just one thing to remember: `MapView` extends 
[GestureLayout](mapview/src/main/java/com/peterlaurence/mapview/layout/GestureLayout.kt) and this 
last class has a ton of features (the source code is well documented). You can:

* add listeners to events like pan, fling, zoom..
* programmatically scroll and center to a position
* respond to various touch events by subclassing `MapView` and overload related methods declared in `GestureLayout`

This list isn't complete. A dedicated section will be added.

### <a name="TOC-MapViewConfiguration"></a> MapViewConfiguration

The MapView must be configured using a `MapViewConfiguration`. It holds the mandatory parameters to 
build a MapView.

Then, you can set optional properties by calling available methods on your `MapViewConfiguration` 
instance. Here is an example:

```kotlin
val config = MapViewConfiguration(levelCount = 7, fullWidth = 25000, fullHeight = 12500,
                                  tileSize = 256, tileStreamProvider = tileStreamProvider)
                                  .setMaxScale(2f)
```

See documentation [here](https://github.com/peterLaurence/MapView/blob/b9e89f8f179f109b1c0843b19d53c8f9c7f2689c/mapview/src/main/java/com/peterlaurence/mapview/MapView.kt#L340). 
Below is a description of mandatory parameters:

**`levelCount`**

The provided `MapViewConfiguration.levelCount` will define the zoomLevels index that the provided 
`MapViewConfiguration.tileStreamProvider` will be given for its `TileStreamProvider.zoomLevels`.
The zoomLevels will be in the range [0 ; `MapViewConfiguration.levelCount`-1].

**`fullWidth` and `fullHeight`**

These are respectively the width and height in pixels of the map _at scale 1_ (that is, the width 
and height of the last level).
In other words, if you put together all the tiles of the last level, you would obtain a big image. 
`fullWidth` and `fullHeight` are dimensions in pixels of this big image.

**`tileSize`**

The size of the tiles in pixels, which are assumed to be squares and always of the same size for all
levels. For now, MapView doesn't support rectangular tiles or tiles of heterogeneous sizes.

**`tileStreamProvider`**

See the section below.

### <a name="TOC-TileStreamProvider"></a> TileStreamProvider

The MapView will request tiles using the convention that each levels has its tiles organized like this:

<p align="center">
<img src="doc/readme-files/tilematrix.png">
</p>

However, MapView isn't opinionated about that. This is one of the purpose of this `TileStreamProvider`:
```kotlin
interface TileStreamProvider {
    fun getTileStream(row: Int, col: Int, zoomLvl: Int): InputStream?
}
```
Your implementation of this interface does the necessary coordinate translation (if required). This is 
where you do your HTTP request if you have remote tiles, or fetch from a local database (or file system).

*Tile caching*

The MapView leverages bitmap pooling to reduce the pressure on the garbage collector. However, 
there's no tile caching by default - this is an implementation detail of the supplied 
`TileStreamProvider`.

### <a name="TOC-ReferentialOwner"></a> ReferentialOwner

When the scale and/or the rotation of the MapView change, some of the child views might have to change
accordingly. For that purpose, you can register a `ReferentialOwner` to the MapView.

A `ReferentialOwner` is an interface:
```kotlin
interface ReferentialOwner {
    var referentialData: ReferentialData
}
```
And `ReferentialData` holds several useful properties:
```kotlin
data class ReferentialData(var rotationEnabled: Boolean = true,
                           var angle: AngleDegree = 0f,
                           var scale: Float = 0f,
                           var centerX: Double = 0.0,
                           var centerY: Double = 0.0) : Parcelable
```
A `ReferentialOwner` should be registered to the MapView:
```kotlin
mapView.addReferentialOwner(refOwner)
// If you need to unregister it:
mapView.removeReferentialOwner(refOwner)
```
From inside your `ReferentialOwner` implementation, you can have any logic you want. You can rotate
some markers, rotate complex views taking into account the `centerX` and `centerY` properties, etc.

There's an example of usage at [RotatingMapFragment](demo/src/main/java/com/peterlaurence/mapview/demo/fragments/RotatingMapFragment.kt).

## Create a deep-zoom map

If you don't already have such a map and you need to make one from a big image, follow this [tutorial](doc/libvips.md).

## How do I..

Follow this [cheat sheet](doc/how-do-i.md).

### API documentation

API documentation has its own [wiki page](https://github.com/peterLaurence/MapView/wiki/MapView-API).




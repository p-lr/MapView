@file:Suppress("unused")

package ovh.plrapps.mapview.paths


import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.TypedValue
import android.view.View
import ovh.plrapps.mapview.MapView
import ovh.plrapps.mapview.ReferentialData
import ovh.plrapps.mapview.ReferentialListener

/**
 * This is a custom view that uses [Canvas.drawLines] to draw a path.
 * This method is much more efficient as it's hardware accelerated under some circumstances, although the result is not
 * as neat as the output of [Path].
 *
 * @author peterLaurence on 19/02/17 -- Converted to Kotlin on 26/07/19
 */
class PathView(context: Context) : View(context), ReferentialListener {
    private val strokeWidthDefault: Float

    private var referentialData = ReferentialData(false)
        set(value) {
            field = value
            invalidate()
        }

    private var shouldDraw = true

    private var pathList: List<DrawablePath>? = null

    private val defaultPaint = Paint()

    /**
     * The color of the default [Paint] that is assigned to a [DrawablePath] if its [DrawablePath.paint]
     * property isn't set.
     */
    var color: Int
        get() = defaultPaint.color
        set(value) {
            defaultPaint.color = value
        }

    init {
        setWillNotDraw(false)

        val metrics = resources.displayMetrics
        strokeWidthDefault =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DEFAULT_STROKE_WIDTH_DP.toFloat(), metrics)

        defaultPaint.style = Paint.Style.STROKE
        defaultPaint.color = DEFAULT_STROKE_COLOR
        defaultPaint.strokeWidth = strokeWidthDefault
        // As of 22/06/19, the settings below cause performance drop when long paths are rendered
        //        mDefaultPaint.setAntiAlias(true);
        //        mDefaultPaint.setStrokeJoin(Paint.Join.ROUND);
        //        mDefaultPaint.setStrokeCap(Paint.Cap.ROUND);
    }

    override fun onReferentialChanged(refData: ReferentialData) {
        referentialData = refData
    }

    fun updatePaths(pathList: List<DrawablePath>) {
        this.pathList = pathList
        invalidate()
    }

    fun setShouldDraw(shouldDraw: Boolean) {
        this.shouldDraw = shouldDraw
        invalidate()
    }

    public override fun onDraw(canvas: Canvas) {
        val pathList = this.pathList
        if (shouldDraw && pathList != null) {

            /* If there is rotation data, take it into account */
            val rd = referentialData
            if (rd.rotationEnabled) {
                canvas.rotate(rd.angle, (width * rd.centerX).toFloat(), (height * rd.centerY).toFloat())
            }

            canvas.scale(rd.scale, rd.scale)
            for (path in pathList) {
                /* If no Paint is defined, take the default one */
                if (path.paint == null) {
                    path.paint = defaultPaint
                }

                /* If no width is defined, take the default one */
                val width = path.width ?: strokeWidthDefault

                if (path.visible) {
                    path.paint?.let {
                        it.strokeWidth = width / rd.scale
                        canvas.drawLines(path.path, path.offset, path.count, it)
                    }
                }
            }
        }
        super.onDraw(canvas)
    }

    interface DrawablePath {
        /**
         * Whether or not this path should be drawn
         */
        val visible: Boolean

        /**
         * The path. See [Canvas.drawLines].
         */
        var path: FloatArray

        /**
         * The paint to be used for this path.
         */
        var paint: Paint?

        /**
         * The width of the path
         */
        val width: Float?

        /**
         * The number of values in the [path] array to skip. It should be a multiple of 4.
         */
        val offset: Int
            get() = 0

        /**
         * The number of values in the array to process, after skipping [offset] of them.
         * Beware that [count] + [offset] shouldn't exceed the [path] length, or an exception will
         * be thrown.
         */
        val count: Int
            get() = path.size
    }
}

private const val DEFAULT_STROKE_COLOR = -0xc0ae4b
private const val DEFAULT_STROKE_WIDTH_DP = 4

/**
 * Represents a point of a path. The coordinates are relative, and the [MapView.coordinateTranslater] will be used to
 * transform them to pixel coordinates.
 *
 * @param x relative x coordinate
 * @param y relative y coordinate
 */
data class PathPoint(val x: Double, val y: Double)

/**
 * Helper function to transform a list of [PathPoint] into a [FloatArray], needed for [Canvas.drawLines].
 */
fun List<PathPoint>.toFloatArray(mapView: MapView): FloatArray? {
    /* If there is only one point, the path has no sense */
    if (size < 2) return null

    val size = size * 4 - 4
    val lines = FloatArray(size)

    var i = 0
    var init = true
    val coordinateTranslater = mapView.coordinateTranslater ?: return null
    for (point in this) {
        if (init) {
            lines[i] = coordinateTranslater.translateX(point.x).toFloat()
            lines[i + 1] = coordinateTranslater.translateY(point.y).toFloat()
            init = false
            i += 2
        } else {
            lines[i] = coordinateTranslater.translateX(point.x).toFloat()
            lines[i + 1] = coordinateTranslater.translateY(point.y).toFloat()
            if (i + 2 >= size) break
            lines[i + 2] = lines[i]
            lines[i + 3] = lines[i + 1]
            i += 4
        }
    }

    return lines
}

/**
 * Helper function to correctly add a [PathView] to the [MapView].
 */
fun MapView.addPathView(pathView: PathView) {
    addView(pathView, 1)
    addReferentialListener(pathView)
}

/**
 * Helper function to correctly remove a [PathView] from the [MapView].
 */
fun MapView.removePathView(pathView: PathView) {
    removeView(pathView)
    removeReferentialListener(pathView)
}
package ovh.plrapps.mapview.layout.detectors

import android.view.MotionEvent
import ovh.plrapps.mapview.layout.detectors.RotationGestureDetector.OnRotationGestureListener
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Detects rotation transformation gestures using the supplied [MotionEvent]s.
 * The [OnRotationGestureListener] callback will notify clients when a particular gesture event has
 * occurred.
 *
 * Usage:
 *  * Create an instance of the `RotationGestureDetector` for your view.
 *  * In the `onTouchEvent` of your view, ensure you call [RotationGestureDetector.onTouchEvent].
 *
 * @param listener the listener invoked for all related callbacks
 * @param threshold the minimum rotation for the rotation gesture to be accounted for
 *
 * @author P.Laurence on 12/12/19
 */
class RotationGestureDetector(private val listener: OnRotationGestureListener,
                              private val threshold: Float = 5f) {
    private var initialAngle = 0f
    private var currAngle = 0f
    private var prevAngle = 0f

    /**
     * The rotation delta in degrees from the previous rotation event to the current event.
     */
    private val rotationDelta: Float
        get() = currAngle - prevAngle

    /**
     * The X coordinate of the current gesture's focal point, in pixels. If a gesture is in progress,
     * the focal point is between each of the pointers forming the gesture.
     * If [isInProgress] would return false, the result of this function is undefined.
     */
    private var focusX = 0f

    /**
     * The Y coordinate of the current gesture's focal point, in pixels. If a gesture is in progress,
     * the focal point is between each of the pointers forming the gesture.
     * If [isInProgress] would return false, the result of this function is undefined.
     */
    private var focusY = 0f

    /**
     * `true` if a rotation gesture is in progress
     */
    var isInProgress = false
        private set

    private var isGestureAccepted = false

    /**
     * Accepts MotionEvents and dispatches events to a [OnRotationGestureListener]
     * when appropriate.
     *
     * Applications should pass a complete and consistent event stream to this method.
     * A complete and consistent event stream involves all MotionEvents from the initial
     * ACTION_DOWN to the final ACTION_UP or ACTION_CANCEL.
     *
     * @param event The event to process
     * @return true if the event was processed and the detector wants to receive the rest of the
     * MotionEvents in this event stream.
     */
    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> cancelRotation()
            MotionEvent.ACTION_POINTER_DOWN -> if (event.pointerCount == 2) { // Second finger is placed
                currAngle = computeRotation(event)
                prevAngle = currAngle
                initialAngle = prevAngle
            }
            MotionEvent.ACTION_MOVE -> if (event.pointerCount >= 2 && (!isInProgress || isGestureAccepted)) { // Moving 2 or more fingers on the screen
                currAngle = computeRotation(event)
                focusX = 0.5f * (event.getX(1) + event.getX(0))
                focusY = 0.5f * (event.getY(1) + event.getY(0))
                val isAlreadyStarted = isInProgress
                tryStartRotation()
                val isAccepted = !isAlreadyStarted || processRotation()
                if (isAccepted) {
                    prevAngle = currAngle
                }
            }
            MotionEvent.ACTION_POINTER_UP -> if (event.pointerCount == 2) { // Only one finger is left
                cancelRotation()
            }
            else -> {
            }
        }
        return true
    }

    private fun tryStartRotation() {
        if (isInProgress || abs(initialAngle - currAngle) < threshold) return

        isInProgress = true
        isGestureAccepted = listener.onRotationBegin()
    }

    private fun cancelRotation() {
        if (!isInProgress) return

        isInProgress = false
        if (isGestureAccepted) {
            listener.onRotationEnd()
            isGestureAccepted = false
        }
    }

    private fun processRotation(): Boolean {
        return isInProgress && isGestureAccepted && listener.onRotate(rotationDelta, focusX, focusY)
    }

    private fun computeRotation(event: MotionEvent): Float {
        return Math.toDegrees(
            atan2(
                event.getY(1) - event.getY(0).toDouble(), event.getX(1) - event.getX(0).toDouble()
            )
        ).toFloat()
    }

    /**
     * The listener for receiving notifications when gestures occur.
     *
     * An application will receive events in the following order:
     *  * One [OnRotationGestureListener.onRotationBegin]
     *  * Zero or more [OnRotationGestureListener.onRotate]
     *  * One [OnRotationGestureListener.onRotationEnd]
     */
    interface OnRotationGestureListener {
        /**
         * Reports back the [rotationDelta], [focusX] and [focusY]
         * @param rotationDelta in degrees
         *
         * @return Whether or not the detector should consider this event as handled. If an event
         * was not handled, the detector will continue to accumulate movement until an event is
         * handled. This can be useful if an application, for example, only wants to update rotation
         * angle if the change is greater than 0.01.
         */
        fun onRotate(rotationDelta: Float, focusX: Float, focusY: Float): Boolean

        /**
         * Indicates that a rotation gesture begins.
         *
         * @return Whether or not the detector should continue recognizing this gesture.
         * For example, if a gesture is beginning with a focal point outside of a region where
         * it makes sense, onRotationBegin() may return false to ignore the rest of the gesture.
         */
        fun onRotationBegin(): Boolean

        /**
         * Indicates that a rotation gesture ends.
         */
        fun onRotationEnd()
    }
}
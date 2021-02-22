package ovh.plrapps.mapview.layout.detectors

import android.view.MotionEvent


class TouchUpGestureDetector(private val mOnTouchUpListener: OnTouchUpListener?) {

    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP) {
            if (mOnTouchUpListener != null) {
                return mOnTouchUpListener.onTouchUp(event)
            }
        }
        return true
    }

    interface OnTouchUpListener {
        fun onTouchUp(event: MotionEvent): Boolean
    }
}
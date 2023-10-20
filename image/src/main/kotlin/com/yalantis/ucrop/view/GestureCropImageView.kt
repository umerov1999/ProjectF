package com.yalantis.ucrop.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener
import com.yalantis.ucrop.util.RotationGestureDetector
import com.yalantis.ucrop.util.RotationGestureDetector.SimpleOnRotationGestureListener
import kotlin.math.pow

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
class GestureCropImageView : CropImageView {
    var mMidPntX = 0f
    var mMidPntY = 0f
    private var mScaleDetector: ScaleGestureDetector? = null
    private var mRotateDetector: RotationGestureDetector? = null
    private var mGestureDetector: GestureDetector? = null
    var isRotateEnabled = true
    var isScaleEnabled = true
    var doubleTapScaleSteps = 5

    constructor(context: Context) : super(context)

    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int = 0) : super(
        context,
        attrs,
        defStyle
    )

    /**
     * If it's ACTION_DOWN event - user touches the screen and all current animation must be canceled.
     * If it's ACTION_UP event - user removed all fingers from the screen and current image position must be corrected.
     * If there are more than 2 fingers - update focal point coordinates.
     * Pass the event to the gesture detectors if those are enabled.
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_DOWN) {
            cancelAllAnimations()
        }
        if (event.pointerCount > 1) {
            mMidPntX = (event.getX(0) + event.getX(1)) / 2
            mMidPntY = (event.getY(0) + event.getY(1)) / 2
        }
        mGestureDetector?.onTouchEvent(event)
        if (isScaleEnabled) {
            mScaleDetector?.onTouchEvent(event)
        }
        if (isRotateEnabled) {
            mRotateDetector?.onTouchEvent(event)
        }
        if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {
            setImageToWrapCropBounds()
        }
        return true
    }

    init {
        setupGestureListeners()
    }

    internal val doubleTapTargetScale: Float
        /**
         * This method calculates target scale value for double tap gesture.
         * User is able to zoom the image from min scale value
         * to the max scale value with [.mDoubleTapScaleSteps] double taps.
         */
        get() = currentScale * (maxScale / minScale).toDouble()
            .pow((1.0f / doubleTapScaleSteps).toDouble()).toFloat()

    private fun setupGestureListeners() {
        mGestureDetector = GestureDetector(context, GestureListener(), null, true)
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mRotateDetector = RotationGestureDetector(RotateListener())
    }

    internal inner class ScaleListener : SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            postScale(detector.scaleFactor, mMidPntX, mMidPntY)
            return true
        }
    }

    internal inner class GestureListener : SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            zoomImageToPosition(doubleTapTargetScale, e.x, e.y, DOUBLE_TAP_ZOOM_DURATION.toLong())
            return super.onDoubleTap(e)
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            postTranslate(-distanceX, -distanceY)
            return true
        }
    }

    internal inner class RotateListener : SimpleOnRotationGestureListener() {
        override fun onRotation(rotationDetector: RotationGestureDetector): Boolean {
            postRotate(rotationDetector.angle, mMidPntX, mMidPntY)
            return true
        }
    }

    companion object {
        private const val DOUBLE_TAP_ZOOM_DURATION = 200
    }
}

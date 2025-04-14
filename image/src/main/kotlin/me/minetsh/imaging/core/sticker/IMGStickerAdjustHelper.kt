package me.minetsh.imaging.core.sticker

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import me.minetsh.imaging.view.IMGStickerView
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Created by felix on 2017/11/15 下午5:44.
 */
class IMGStickerAdjustHelper(private val mContainer: IMGStickerView, private val mView: View) :
    OnTouchListener {
    private val M = Matrix()
    private var mRadius = 0.0
    private var mDegrees = 0.0

    init {
        mView.setOnTouchListener(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        var mCenterY: Float
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                val mCenterX: Float = 0.also { mCenterY = it.toFloat() }.toFloat()
                val pointX = mView.x + x - mContainer.pivotX
                val pointY = mView.y + y - mContainer.pivotY
                Log.d(TAG, String.format("X=%f,Y=%f", pointX, pointY))
                mRadius = toLength(0f, 0f, pointX, pointY)
                mDegrees = toDegrees(pointY, pointX)
                M.setTranslate(pointX - x, pointY - y)
                Log.d(TAG, String.format("degrees=%f", toDegrees(pointY, pointX)))
                M.postRotate(-toDegrees(pointY, pointX).toFloat(), mCenterX, mCenterY)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val xy = floatArrayOf(event.x, event.y)
                val pointX = mView.x + xy[0] - mContainer.pivotX
                val pointY = mView.y + xy[1] - mContainer.pivotY
                Log.d(TAG, String.format("X=%f,Y=%f", pointX, pointY))
                val radius = toLength(0f, 0f, pointX, pointY)
                val degrees = toDegrees(pointY, pointX)
                val scale = (radius / mRadius).toFloat()
                mContainer.addScale(scale)
                Log.d(TAG, "    D   = " + (degrees - mDegrees))
                mContainer.rotation = (mContainer.rotation + degrees - mDegrees).toFloat()
                mRadius = radius
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAG = "IMGStickerAdjustHelper"
        internal fun toDegrees(v: Float, v1: Float): Double {
            return Math.toDegrees(atan2(v.toDouble(), v1.toDouble()))
        }

        internal fun toLength(x1: Float, y1: Float, x2: Float, y2: Float): Double {
            return sqrt(((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)).toDouble())
        }
    }
}

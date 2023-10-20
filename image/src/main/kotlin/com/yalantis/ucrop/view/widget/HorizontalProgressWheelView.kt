package com.yalantis.ucrop.view.widget

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import me.minetsh.imaging.R

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 */
class HorizontalProgressWheelView : View {
    private val mCanvasClipBounds = Rect()
    private var mScrollingListener: ScrollingListener? = null
    private var mLastTouchedPosition = 0f
    private var mProgressLinePaint: Paint? = null
    private var mProgressMiddleLinePaint: Paint? = null
    private var mProgressLineWidth = 0
    private var mProgressLineHeight = 0
    private var mProgressLineMargin = 0
    private var mScrollStarted = false
    private var mTotalScrollDistance = 0f
    private var mMiddleLineColor = 0

    @JvmOverloads
    constructor(context: Context?, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    fun setScrollingListener(scrollingListener: ScrollingListener?) {
        mScrollingListener = scrollingListener
    }

    fun setMiddleLineColor(@ColorInt middleLineColor: Int) {
        mMiddleLineColor = middleLineColor
        mProgressMiddleLinePaint?.color = mMiddleLineColor
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> mLastTouchedPosition = event.x
            MotionEvent.ACTION_UP -> if (mScrollingListener != null) {
                mScrollStarted = false
                mScrollingListener?.onScrollEnd()
            }

            MotionEvent.ACTION_MOVE -> {
                val distance = event.x - mLastTouchedPosition
                if (distance != 0f) {
                    if (!mScrollStarted) {
                        mScrollStarted = true
                        mScrollingListener?.onScrollStart()
                    }
                    onScrollEvent(event, distance)
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.getClipBounds(mCanvasClipBounds)
        val linesCount = mCanvasClipBounds.width() / (mProgressLineWidth + mProgressLineMargin)
        val deltaX = mTotalScrollDistance % (mProgressLineMargin + mProgressLineWidth).toFloat()
        for (i in 0 until linesCount) {
            if (i < linesCount / 4) {
                mProgressLinePaint?.alpha = (255 * (i / (linesCount / 4).toFloat())).toInt()
            } else if (i > linesCount * 3 / 4) {
                mProgressLinePaint?.alpha =
                    (255 * ((linesCount - i) / (linesCount / 4).toFloat())).toInt()
            } else {
                mProgressLinePaint?.alpha = 255
            }
            mProgressLinePaint?.let {
                canvas.drawLine(
                    -deltaX + mCanvasClipBounds.left + i * (mProgressLineWidth + mProgressLineMargin),
                    mCanvasClipBounds.centerY() - mProgressLineHeight / 4.0f,
                    -deltaX + mCanvasClipBounds.left + i * (mProgressLineWidth + mProgressLineMargin),
                    mCanvasClipBounds.centerY() + mProgressLineHeight / 4.0f, it
                )
            }
        }
        mProgressMiddleLinePaint?.let {
            canvas.drawLine(
                mCanvasClipBounds.centerX().toFloat(),
                mCanvasClipBounds.centerY() - mProgressLineHeight / 2.0f,
                mCanvasClipBounds.centerX().toFloat(),
                mCanvasClipBounds.centerY() + mProgressLineHeight / 2.0f,
                it
            )
        }
    }

    private fun onScrollEvent(event: MotionEvent, distance: Float) {
        mTotalScrollDistance -= distance
        postInvalidate()
        mLastTouchedPosition = event.x
        mScrollingListener?.onScroll(-distance, mTotalScrollDistance)
    }

    private fun init() {
        mMiddleLineColor =
            ContextCompat.getColor(context, R.color.ucrop_color_widget_rotate_mid_line)
        mProgressLineWidth =
            context.resources.getDimensionPixelSize(R.dimen.ucrop_width_horizontal_wheel_progress_line)
        mProgressLineHeight =
            context.resources.getDimensionPixelSize(R.dimen.ucrop_height_horizontal_wheel_progress_line)
        mProgressLineMargin =
            context.resources.getDimensionPixelSize(R.dimen.ucrop_margin_horizontal_wheel_progress_line)
        mProgressLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mProgressLinePaint?.style = Paint.Style.STROKE
        mProgressLinePaint?.strokeWidth = mProgressLineWidth.toFloat()
        mProgressLinePaint?.color = ContextCompat.getColor(
            context,
            R.color.ucrop_color_progress_wheel_line
        )
        mProgressMiddleLinePaint = Paint(mProgressLinePaint)
        mProgressMiddleLinePaint?.color = mMiddleLineColor
        mProgressMiddleLinePaint?.strokeCap = Paint.Cap.ROUND
        mProgressMiddleLinePaint?.strokeWidth =
            context.resources.getDimensionPixelSize(R.dimen.ucrop_width_middle_wheel_progress_line)
                .toFloat()
    }

    interface ScrollingListener {
        fun onScrollStart()
        fun onScroll(delta: Float, totalDistance: Float)
        fun onScrollEnd()
    }
}

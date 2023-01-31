package de.maxr1998.modernpreferences.preferences.colorpicker.slider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DimenRes
import de.maxr1998.modernpreferences.R

abstract class AbsCustomSlider : View {
    protected var bitmap: Bitmap? = null
    private var bitmapCanvas: Canvas? = null
    protected var bar: Bitmap? = null
    private var barCanvas: Canvas? = null
    private var valueChangedListener: OnValueChangedListener? = null
    private var barOffsetX = 0
    protected var handleRadius = 20
    protected var barHeight = 5
    protected var value = 1f
    protected var mShowBorder = false
    private var inVerticalOrientation = false

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val styledAttrs = context.theme.obtainStyledAttributes(
            attrs, R.styleable.AbsCustomSlider, 0, 0
        )
        inVerticalOrientation = try {
            styledAttrs.getBoolean(
                R.styleable.AbsCustomSlider_inVerticalOrientation, inVerticalOrientation
            )
        } finally {
            styledAttrs.recycle()
        }
    }

    protected fun updateBar() {
        handleRadius = getDimension(R.dimen.default_slider_handler_radius)
        barHeight = getDimension(R.dimen.default_slider_bar_height)
        barOffsetX = handleRadius
        if (bar == null) createBitmaps()
        drawBar(barCanvas)
        invalidate()
    }

    protected open fun createBitmaps() {
        val width: Int
        val height: Int
        if (inVerticalOrientation) {
            width = getHeight()
            height = getWidth()
        } else {
            width = getWidth()
            height = getHeight()
        }
        bar = Bitmap.createBitmap(
            (width - barOffsetX * 2).coerceAtLeast(1),
            barHeight,
            Bitmap.Config.ARGB_8888
        )
        barCanvas = Canvas(bar ?: return)
        if (bitmap == null || bitmap?.width != width || bitmap?.height != height) {
            if (bitmap != null) bitmap?.recycle()
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmapCanvas = Canvas(bitmap ?: return)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width: Int
        val height: Int
        if (inVerticalOrientation) {
            width = getHeight()
            height = getWidth()
            canvas.rotate(-90f)
            canvas.translate(-width.toFloat(), 0f)
        } else {
            width = getWidth()
            height = getHeight()
        }
        if (bar != null && bitmapCanvas != null) {
            (bitmapCanvas ?: return).drawColor(0, PorterDuff.Mode.CLEAR)
            (bitmapCanvas ?: return).drawBitmap(
                bar ?: return,
                barOffsetX.toFloat(),
                (height - (bar ?: return).height).toFloat() / 2,
                null
            )
            val x = handleRadius + value * (width - handleRadius * 2)
            val y = height / 2f
            drawHandle(bitmapCanvas ?: return, x, y)
            canvas.drawBitmap(bitmap ?: return, 0f, 0f, null)
        }
    }

    protected abstract fun drawBar(barCanvas: Canvas?)
    protected abstract fun onValueChanged(value: Float)
    protected abstract fun drawHandle(canvas: Canvas, x: Float, y: Float)
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateBar()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var width = 0
        when (widthMode) {
            MeasureSpec.UNSPECIFIED -> width =
                widthMeasureSpec

            MeasureSpec.AT_MOST -> width =
                MeasureSpec.getSize(widthMeasureSpec)

            MeasureSpec.EXACTLY -> width =
                MeasureSpec.getSize(widthMeasureSpec)
        }
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        var height = 0
        when (heightMode) {
            MeasureSpec.UNSPECIFIED -> height =
                heightMeasureSpec

            MeasureSpec.AT_MOST -> height =
                MeasureSpec.getSize(heightMeasureSpec)

            MeasureSpec.EXACTLY -> height =
                MeasureSpec.getSize(heightMeasureSpec)
        }
        setMeasuredDimension(width, height)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (bar != null) {
                    value = if (inVerticalOrientation) {
                        1 - (event.y - barOffsetX) / bar!!.width
                    } else {
                        (event.x - barOffsetX) / bar!!.width
                    }
                    value = 0f.coerceAtLeast(value.coerceAtMost(1f))
                    onValueChanged(value)
                    invalidate()
                }
            }

            MotionEvent.ACTION_UP -> {
                onValueChanged(value)
                valueChangedListener?.onValueChanged(value)
                invalidate()
            }
        }
        return true
    }

    private fun getDimension(@DimenRes id: Int): Int {
        return resources.getDimensionPixelSize(id)
    }

    fun setShowBorder(mShowBorder: Boolean) {
        this.mShowBorder = mShowBorder
    }

    fun setOnValueChangedListener(valueChangedListener: OnValueChangedListener?) {
        this.valueChangedListener = valueChangedListener
    }
}
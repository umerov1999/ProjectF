package dev.ragnarok.fenrir.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import dev.ragnarok.fenrir.R

class RoundCornerRelativeView : RelativeLayout {
    private val VIEW_PAINT = Paint(Paint.ANTI_ALIAS_FLAG)
    private val PATH = Path()
    val DEFAULT_RADIUS = 12f
    val DEFAULT_VIEW_COLOR = Color.RED
    val DEFAULT_VIEW_ALPHA = 255
    val DEFAULT_IS_STROKE = false
    val DEFAULT_STROKE_WIDTH = 1f
    private var radius_top_left = DEFAULT_RADIUS
    private var radius_top_right = DEFAULT_RADIUS
    private var radius_bottom_left = DEFAULT_RADIUS
    private var radius_bottom_right = DEFAULT_RADIUS
    private var viewColor = DEFAULT_VIEW_COLOR
    private var viewAlpha = DEFAULT_VIEW_ALPHA
    private var isStroke = DEFAULT_IS_STROKE
    private var strokeWidth = DEFAULT_STROKE_WIDTH

    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setWillNotDraw(false)
        VIEW_PAINT.isDither = true
        VIEW_PAINT.isAntiAlias = true
        initializeAttributes(context, attrs)
    }

    private fun dp2px(dpValue: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue,
            resources.displayMetrics
        )
    }

    @SuppressLint("CustomViewStyleable")
    private fun initializeAttributes(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.RoundCornerLinearView)
            radius_top_left = array.getDimension(
                R.styleable.RoundCornerLinearView_radius_top_left,
                dp2px(DEFAULT_RADIUS)
            )
            radius_top_right = array.getDimension(
                R.styleable.RoundCornerLinearView_radius_top_right,
                dp2px(DEFAULT_RADIUS)
            )
            radius_bottom_left = array.getDimension(
                R.styleable.RoundCornerLinearView_radius_bottom_left,
                dp2px(DEFAULT_RADIUS)
            )
            radius_bottom_right = array.getDimension(
                R.styleable.RoundCornerLinearView_radius_bottom_right,
                dp2px(DEFAULT_RADIUS)
            )
            viewColor =
                array.getColor(R.styleable.RoundCornerLinearView_view_color, DEFAULT_VIEW_COLOR)
            viewAlpha =
                array.getInt(R.styleable.RoundCornerLinearView_view_alpha, DEFAULT_VIEW_ALPHA)
            isStroke = array.getBoolean(
                R.styleable.RoundCornerLinearView_view_is_stroke,
                DEFAULT_IS_STROKE
            )
            strokeWidth = array.getDimension(
                R.styleable.RoundCornerLinearView_view_stroke_width,
                dp2px(DEFAULT_STROKE_WIDTH)
            )
            array.recycle()
        }
    }

    fun setViewColor(@ColorInt viewColor: Int) {
        this.viewColor = viewColor
        invalidate()
    }

    fun setViewAlpha(viewAlpha: Int) {
        this.viewAlpha = viewAlpha
        invalidate()
    }

    fun setStrokeWidth(strokeWidth: Float) {
        this.strokeWidth = strokeWidth
        invalidate()
    }

    fun setIsStroke(isStroke: Boolean) {
        this.isStroke = isStroke
        invalidate()
    }

    fun setRadiusTopLeft(radius_top_left: Float) {
        this.radius_top_left = radius_top_left
        invalidate()
    }

    fun setRadiusTopRight(radius_top_right: Float) {
        this.radius_top_right = radius_top_right
        invalidate()
    }

    fun setRadiusBottomLeft(radius_bottom_left: Float) {
        this.radius_bottom_left = radius_bottom_left
        invalidate()
    }

    fun setRadiusBottomRight(radius_bottom_right: Float) {
        this.radius_bottom_right = radius_bottom_right
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val widthTmp = width - strokeWidth
        val heightTmp = height - strokeWidth
        if (widthTmp <= 0 || heightTmp <= 0) {
            return
        }
        VIEW_PAINT.color = viewColor
        VIEW_PAINT.alpha = viewAlpha
        VIEW_PAINT.style = if (isStroke) Paint.Style.STROKE else Paint.Style.FILL
        VIEW_PAINT.strokeWidth = strokeWidth
        VIEW_PAINT.shader = null
        VIEW_PAINT.strokeCap = Paint.Cap.ROUND
        VIEW_PAINT.strokeJoin = Paint.Join.ROUND
        PATH.reset()
        PATH.fillType = Path.FillType.EVEN_ODD
        PATH.moveTo(strokeWidth, radius_top_left)
        PATH.arcTo(
            strokeWidth,
            strokeWidth,
            2 * radius_top_left,
            2 * radius_top_left,
            180f,
            90f,
            false
        )
        PATH.lineTo(widthTmp - radius_top_right, strokeWidth)
        PATH.arcTo(
            widthTmp - 2 * radius_top_right,
            strokeWidth,
            widthTmp,
            2 * radius_top_right,
            270f,
            90f,
            false
        )
        PATH.lineTo(widthTmp, heightTmp - radius_bottom_right)
        PATH.arcTo(
            widthTmp - 2 * radius_bottom_right,
            heightTmp - 2 * radius_bottom_right,
            widthTmp,
            heightTmp,
            0f,
            90f,
            false
        )
        PATH.lineTo(radius_bottom_left, heightTmp)
        PATH.arcTo(
            strokeWidth,
            heightTmp - 2 * radius_bottom_left,
            2 * radius_bottom_left,
            heightTmp,
            90f,
            90f,
            false
        )
        PATH.lineTo(strokeWidth, radius_top_left)
        PATH.close()
        canvas.drawPath(PATH, VIEW_PAINT)
    }
}

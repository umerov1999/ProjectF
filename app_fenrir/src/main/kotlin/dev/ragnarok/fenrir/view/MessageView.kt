package dev.ragnarok.fenrir.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import dev.ragnarok.fenrir.R

class MessageView : LinearLayout {
    private val FILL_PAINT = Paint(Paint.ANTI_ALIAS_FLAG)
    private val PATH = Path()
    private val DEFAULT_RADIUS = 10f
    private var radius_top_left = DEFAULT_RADIUS
    private var radius_top_right = DEFAULT_RADIUS
    private var radius_bottom_left = DEFAULT_RADIUS
    private var radius_bottom_right = DEFAULT_RADIUS
    private var first_color = DEFAULT_COLOR
    private var second_color = DEFAULT_COLOR
    private var canvasWidth = 0
    private var canvasHeight = 0
    private var gradient: LinearGradient? = null

    constructor(context: Context?) : super(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        setWillNotDraw(false)
        FILL_PAINT.style = Paint.Style.FILL
        FILL_PAINT.isDither = true
        FILL_PAINT.isAntiAlias = true
        initializeAttributes(context, attrs)
    }

    private fun dp2px(dpValue: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue,
            resources.displayMetrics
        )
    }

    private fun initializeAttributes(context: Context, attrs: AttributeSet?) {
        if (attrs != null) {
            val array = context.obtainStyledAttributes(attrs, R.styleable.MessageView)
            setRadius(array.getDimension(R.styleable.MessageView_radius, dp2px(DEFAULT_RADIUS)))
            first_color = array.getColor(R.styleable.MessageView_first_color, DEFAULT_COLOR)
            second_color = array.getColor(R.styleable.MessageView_second_color, DEFAULT_COLOR)
            array.recycle()
        }
    }

    private fun setRadius(radius: Float) {
        radius_bottom_right = radius
        radius_bottom_left = radius
        radius_top_right = radius
        radius_top_left = radius
    }

    fun setGradientColor(first_color: Int, second_color: Int) {
        this.first_color = first_color
        this.second_color = second_color
        invalidate()
    }

    fun setNonGradientColor(color: Int) {
        first_color = color
        second_color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (first_color == second_color) {
            FILL_PAINT.color = first_color
            FILL_PAINT.shader = null
        } else {
            if (canvasWidth != width || canvasHeight != height || gradient == null) {
                canvasWidth = width
                canvasHeight = height
                gradient = LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    first_color, second_color, Shader.TileMode.CLAMP
                )
            }
            FILL_PAINT.shader = gradient
        }
        val width = width
        val height = height
        PATH.reset()
        PATH.moveTo(0f, radius_top_left)
        PATH.arcTo(0f, 0f, 2 * radius_top_left, 2 * radius_top_left, 180f, 90f, false)
        PATH.lineTo(width - radius_top_right, 0f)
        PATH.arcTo(
            width - 2 * radius_top_right,
            0f,
            width.toFloat(),
            2 * radius_top_right,
            270f,
            90f,
            false
        )
        PATH.lineTo(width.toFloat(), height - radius_bottom_right)
        PATH.arcTo(
            width - 2 * radius_bottom_right,
            height - 2 * radius_bottom_right,
            width.toFloat(),
            height.toFloat(),
            0f,
            90f,
            false
        )
        PATH.lineTo(radius_bottom_left, height.toFloat())
        PATH.arcTo(
            0f,
            height - 2 * radius_bottom_left,
            2 * radius_bottom_left,
            height.toFloat(),
            90f,
            90f,
            false
        )
        PATH.lineTo(0f, radius_top_left)
        PATH.close()
        canvas.drawPath(PATH, FILL_PAINT)
    }

    companion object {
        private const val DEFAULT_COLOR = Color.RED
    }
}
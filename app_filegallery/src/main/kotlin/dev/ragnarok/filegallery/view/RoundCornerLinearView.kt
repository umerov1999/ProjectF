package dev.ragnarok.filegallery.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.LinearLayout
import dev.ragnarok.filegallery.R

class RoundCornerLinearView : LinearLayout {
    private val FILL_PAINT = Paint(Paint.ANTI_ALIAS_FLAG)
    private val PATH = Path()
    val DEFAULT_RADIUS = 12f
    val DEFAULT_COLOR = Color.RED
    private var radius_top_left = DEFAULT_RADIUS
    private var radius_top_right = DEFAULT_RADIUS
    private var radius_bottom_left = DEFAULT_RADIUS
    private var radius_bottom_right = DEFAULT_RADIUS
    private var color = DEFAULT_COLOR

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
            color = array.getColor(R.styleable.RoundCornerLinearView_view_color, DEFAULT_COLOR)
            array.recycle()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val widthTmp = width
        val heightTmp = height
        if (widthTmp <= 0 || heightTmp <= 0) {
            return
        }
        FILL_PAINT.color = color
        FILL_PAINT.shader = null
        PATH.reset()
        PATH.moveTo(0f, radius_top_left)
        PATH.arcTo(0f, 0f, 2 * radius_top_left, 2 * radius_top_left, 180f, 90f, false)
        PATH.lineTo(widthTmp - radius_top_right, 0f)
        PATH.arcTo(
            widthTmp - 2 * radius_top_right,
            0f,
            widthTmp.toFloat(),
            2 * radius_top_right,
            270f,
            90f,
            false
        )
        PATH.lineTo(widthTmp.toFloat(), heightTmp - radius_bottom_right)
        PATH.arcTo(
            widthTmp - 2 * radius_bottom_right,
            heightTmp - 2 * radius_bottom_right,
            widthTmp.toFloat(),
            heightTmp.toFloat(),
            0f,
            90f,
            false
        )
        PATH.lineTo(radius_bottom_left, heightTmp.toFloat())
        PATH.arcTo(
            0f,
            heightTmp - 2 * radius_bottom_left,
            2 * radius_bottom_left,
            heightTmp.toFloat(),
            90f,
            90f,
            false
        )
        PATH.lineTo(0f, radius_top_left)
        PATH.close()
        canvas.drawPath(PATH, FILL_PAINT)
    }
}

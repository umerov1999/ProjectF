package dev.ragnarok.fenrir.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.util.Utils

class SawView(context: Context, attrs: AttributeSet) : View(context, attrs) {
    companion object {
        private val FILL_PAINT = Paint(Paint.ANTI_ALIAS_FLAG)
        private val PATH = Path()
        private const val SIDE_DOWN = 1
        private const val SIDE_UP = 2

        init {
            FILL_PAINT.style = Paint.Style.FILL
            FILL_PAINT.isDither = true
            FILL_PAINT.isAntiAlias = true
        }
    }

    private var mToothPrefWidht = 0f
    private var mBackgroundColor = 0
    private fun initializeAttributes(context: Context, attrs: AttributeSet) {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.SawView)
        try {
            mToothPrefWidht =
                ta.getDimensionPixelSize(R.styleable.SawView_sawToothPrefWidht, pixelOf(8))
                    .toFloat()
            mBackgroundColor = ta.getColor(R.styleable.SawView_sawBackground, Color.GREEN)
        } finally {
            ta.recycle()
        }
    }

    private fun pixelOf(dp: Int): Int {
        return Utils.dpToPx(dp.toFloat(), context).toInt()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        var count = (width.toFloat() / mToothPrefWidht).toInt()
        if (count % 2 == 1) {
            count += 1
        }
        val startH = 0
        val endH = height
        FILL_PAINT.color = mBackgroundColor
        val realToothWidth = width.toFloat() / count.toFloat()
        var offset = 0f
        var side = SIDE_DOWN
        for (i in 0 until count) {
            if (side == SIDE_DOWN) {
                PATH.reset()
                PATH.moveTo(offset, startH.toFloat())
                PATH.lineTo(offset + realToothWidth, startH.toFloat())
                PATH.lineTo(offset, endH.toFloat())

                //canvas.drawLine(offset, endH, offset + realToothWidth, startH, STROKE_PAINT);
            } else {
                PATH.reset()
                PATH.moveTo(offset, startH.toFloat())
                PATH.lineTo(offset + realToothWidth, startH.toFloat())
                PATH.lineTo(offset + realToothWidth, endH.toFloat())
                //canvas.drawLine(offset, startH, offset + realToothWidth, endH, STROKE_PAINT);
            }
            PATH.lineTo(offset, startH.toFloat())
            canvas.drawPath(PATH, FILL_PAINT)
            offset += realToothWidth
            side = if (side == SIDE_DOWN) SIDE_UP else SIDE_DOWN
        }
    }

    init {
        setWillNotDraw(false)
        initializeAttributes(context, attrs)
    }
}
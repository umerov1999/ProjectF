package de.maxr1998.modernpreferences.preferences.colorpicker

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import de.maxr1998.modernpreferences.preferences.colorpicker.builder.PaintBuilder

class ColorCircleDrawable(color: Int) : ColorDrawable(color) {
    private val fillPaint = PaintBuilder.newPaint().style(Paint.Style.FILL).color(0).build()
    private val fillBackPaint =
        PaintBuilder.newPaint().shader(PaintBuilder.createAlphaPatternShader(26)).build()
    private var strokeWidth = 0f
    private val strokePaint =
        PaintBuilder.newPaint().style(Paint.Style.STROKE).stroke(strokeWidth).color(-0x616162)
            .build()

    override fun draw(canvas: Canvas) {
        canvas.drawColor(0)
        val width = bounds.width()
        val radius = width / 2f
        strokeWidth = radius / 8f
        strokePaint.strokeWidth = strokeWidth
        fillPaint.color = color
        canvas.drawCircle(radius, radius, radius - strokeWidth, fillBackPaint)
        canvas.drawCircle(radius, radius, radius - strokeWidth, fillPaint)
        canvas.drawCircle(radius, radius, radius - strokeWidth, strokePaint)
    }

    override fun setColor(color: Int) {
        super.setColor(color)
        invalidateSelf()
    }
}
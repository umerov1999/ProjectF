package de.maxr1998.modernpreferences.preferences.colorpicker.slider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.util.AttributeSet
import de.maxr1998.modernpreferences.preferences.colorpicker.ColorPickerView
import de.maxr1998.modernpreferences.preferences.colorpicker.Utils
import de.maxr1998.modernpreferences.preferences.colorpicker.builder.PaintBuilder
import kotlin.math.roundToInt

class AlphaSlider : AbsCustomSlider {
    private val alphaPatternPaint = PaintBuilder.newPaint().build()
    private val barPaint = PaintBuilder.newPaint().build()
    private val solid = PaintBuilder.newPaint().build()
    private val clearingStroke =
        PaintBuilder.newPaint().color(-0x1).xPerMode(PorterDuff.Mode.CLEAR).build()
    private var pColor = 0
    private var clearBitmap: Bitmap? = null
    private var clearBitmapCanvas: Canvas? = null
    private var colorPicker: ColorPickerView? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun createBitmaps() {
        super.createBitmaps()
        alphaPatternPaint.shader = PaintBuilder.createAlphaPatternShader(barHeight * 2)
        clearBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        clearBitmapCanvas = Canvas(clearBitmap ?: return)
    }

    override fun drawBar(barCanvas: Canvas?) {
        val width = barCanvas?.width ?: 0
        val height = barCanvas?.height ?: 0
        barCanvas?.drawRect(0f, 0f, width.toFloat(), height.toFloat(), alphaPatternPaint)
        val l = 2.coerceAtLeast(width / 256)
        var x = 0
        while (x <= width) {
            val alpha = x.toFloat() / (width - 1)
            barPaint.color = pColor
            barPaint.alpha = (alpha * 255).roundToInt()
            barCanvas?.drawRect(x.toFloat(), 0f, (x + l).toFloat(), height.toFloat(), barPaint)
            x += l
        }
    }

    override fun onValueChanged(value: Float) {
        colorPicker?.setAlphaValue(value)
    }

    override fun drawHandle(canvas: Canvas, x: Float, y: Float) {
        solid.color = pColor
        solid.alpha = (value * 255).roundToInt()
        if (mShowBorder) canvas.drawCircle(x, y, handleRadius.toFloat(), clearingStroke)
        if (value < 1) {
            // this fixes the same artifact issue from ColorPickerView
            // happens when alpha pattern is drawn underneath a circle with the same size
            clearBitmapCanvas?.drawColor(0, PorterDuff.Mode.CLEAR)
            clearBitmapCanvas?.drawCircle(x, y, handleRadius * 0.75f + 4, alphaPatternPaint)
            clearBitmapCanvas?.drawCircle(x, y, handleRadius * 0.75f + 4, solid)
            val clearStroke =
                PaintBuilder.newPaint().color(-0x1).style(Paint.Style.STROKE).stroke(6f)
                    .xPerMode(PorterDuff.Mode.CLEAR).build()
            clearBitmapCanvas?.drawCircle(
                x,
                y,
                handleRadius * 0.75f + clearStroke.strokeWidth / 2,
                clearStroke
            )
            canvas.drawBitmap(clearBitmap ?: return, 0f, 0f, null)
        } else {
            canvas.drawCircle(x, y, handleRadius * 0.75f, solid)
        }
    }

    fun setColorPicker(colorPicker: ColorPickerView?) {
        this.colorPicker = colorPicker
    }

    fun setColor(pColor: Int) {
        this.pColor = pColor
        value = Utils.getAlphaPercent(pColor)
        if (bar != null) {
            updateBar()
            invalidate()
        }
    }
}
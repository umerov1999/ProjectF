package de.maxr1998.modernpreferences.preferences.colorpicker.slider

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.AttributeSet
import de.maxr1998.modernpreferences.preferences.colorpicker.ColorPickerView
import de.maxr1998.modernpreferences.preferences.colorpicker.Utils
import de.maxr1998.modernpreferences.preferences.colorpicker.builder.PaintBuilder

class LightnessSlider : AbsCustomSlider {
    private val barPaint = PaintBuilder.newPaint().build()
    private val solid = PaintBuilder.newPaint().build()
    private val clearingStroke =
        PaintBuilder.newPaint().color(-0x1).xPerMode(PorterDuff.Mode.CLEAR).build()
    private var pColor = 0
    private var colorPicker: ColorPickerView? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun drawBar(barCanvas: Canvas?) {
        val width = (barCanvas ?: return).width
        val height = barCanvas.height
        val hsv = FloatArray(3)
        Color.colorToHSV(pColor, hsv)
        val l = 2.coerceAtLeast(width / 256)
        var x = 0
        while (x <= width) {
            hsv[2] = x.toFloat() / (width - 1)
            barPaint.color = Color.HSVToColor(hsv)
            barCanvas.drawRect(x.toFloat(), 0f, (x + l).toFloat(), height.toFloat(), barPaint)
            x += l
        }
    }

    override fun onValueChanged(value: Float) {
        colorPicker?.setLightness(value)
    }

    override fun drawHandle(canvas: Canvas, x: Float, y: Float) {
        solid.color =
            Utils.colorAtLightness(pColor, value)
        if (mShowBorder) canvas.drawCircle(x, y, handleRadius.toFloat(), clearingStroke)
        canvas.drawCircle(x, y, handleRadius * 0.75f, solid)
    }

    fun setColorPicker(colorPicker: ColorPickerView?) {
        this.colorPicker = colorPicker
    }

    fun setColor(pColor: Int) {
        this.pColor = pColor
        value = Utils.lightnessOfColor(pColor)
        if (bar != null) {
            updateBar()
            invalidate()
        }
    }
}
package de.maxr1998.modernpreferences.preferences.colorpicker.renderer

import de.maxr1998.modernpreferences.preferences.colorpicker.ColorCircle
import kotlin.math.asin
import kotlin.math.roundToInt

abstract class AbsColorWheelRenderer : ColorWheelRenderer {
    protected val colorCircleList: MutableList<ColorCircle> = ArrayList()
    private var colorWheelRenderOption: ColorWheelRenderOption? = null
    override fun initWith(colorWheelRenderOption: ColorWheelRenderOption) {
        this.colorWheelRenderOption = colorWheelRenderOption
        colorCircleList.clear()
    }

    override val renderOption: ColorWheelRenderOption
        get() {
            if (colorWheelRenderOption == null) colorWheelRenderOption = ColorWheelRenderOption()
            return colorWheelRenderOption!!
        }

    override fun colorCircleList(): List<ColorCircle> {
        return colorCircleList
    }

    protected val alphaValueAsInt: Int
        get() = ((colorWheelRenderOption?.alpha ?: 1f) * 255).roundToInt()

    protected fun calcTotalCount(radius: Float, size: Float): Int {
        return 1.coerceAtLeast(
            ((1f - ColorWheelRenderer.GAP_PERCENTAGE) * Math.PI / asin(
                (size / radius).toDouble()
            ) + 0.5f).toInt()
        )
    }
}
package de.maxr1998.modernpreferences.preferences.colorpicker.renderer

import de.maxr1998.modernpreferences.preferences.colorpicker.ColorCircle

interface ColorWheelRenderer {
    fun draw()
    val renderOption: ColorWheelRenderOption
    fun initWith(colorWheelRenderOption: ColorWheelRenderOption)
    fun colorCircleList(): List<ColorCircle>

    companion object {
        const val GAP_PERCENTAGE = 0.025f
    }
}
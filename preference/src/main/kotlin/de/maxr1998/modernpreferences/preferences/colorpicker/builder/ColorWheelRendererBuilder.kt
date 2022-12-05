package de.maxr1998.modernpreferences.preferences.colorpicker.builder

import de.maxr1998.modernpreferences.preferences.colorpicker.ColorPickerView.WHEEL_TYPE
import de.maxr1998.modernpreferences.preferences.colorpicker.renderer.ColorWheelRenderer
import de.maxr1998.modernpreferences.preferences.colorpicker.renderer.FlowerColorWheelRenderer
import de.maxr1998.modernpreferences.preferences.colorpicker.renderer.SimpleColorWheelRenderer

object ColorWheelRendererBuilder {
    fun getRenderer(wheelType: WHEEL_TYPE?): ColorWheelRenderer {
        when (wheelType) {
            WHEEL_TYPE.CIRCLE -> return SimpleColorWheelRenderer()
            WHEEL_TYPE.FLOWER -> return FlowerColorWheelRenderer()
            else -> {}
        }
        throw IllegalArgumentException("wrong WHEEL_TYPE")
    }
}
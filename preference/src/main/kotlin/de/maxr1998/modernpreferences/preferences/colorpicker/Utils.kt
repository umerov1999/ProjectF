package de.maxr1998.modernpreferences.preferences.colorpicker

import android.graphics.Color
import java.util.*
import kotlin.math.roundToInt

object Utils {
    fun getAlphaPercent(argb: Int): Float {
        return Color.alpha(argb) / 255f
    }

    fun alphaValueAsInt(alpha: Float): Int {
        return (alpha * 255).roundToInt()
    }

    fun adjustAlpha(alpha: Float, color: Int): Int {
        return alphaValueAsInt(alpha) shl 24 or (0x00ffffff and color)
    }

    fun colorAtLightness(color: Int, lightness: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = lightness
        return Color.HSVToColor(hsv)
    }

    fun lightnessOfColor(color: Int): Float {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        return hsv[2]
    }

    fun getHexString(color: Int, showAlpha: Boolean): String {
        val base = if (showAlpha) -0x1 else 0xFFFFFF
        val format = if (showAlpha) "#%08X" else "#%06X"
        return String.format(format, base and color).uppercase(Locale.getDefault())
    }
}
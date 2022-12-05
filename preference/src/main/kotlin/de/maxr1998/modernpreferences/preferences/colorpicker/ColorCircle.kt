package de.maxr1998.modernpreferences.preferences.colorpicker

import android.graphics.Color

class ColorCircle(x: Float, y: Float, hsv: FloatArray) {
    val hsv = FloatArray(3)
    var x = 0f
        private set
    var y = 0f
        private set
    private var hsvClone: FloatArray? = null
    var color = 0
        private set

    fun sqDist(x: Float, y: Float): Double {
        val dx = (this.x - x).toDouble()
        val dy = (this.y - y).toDouble()
        return dx * dx + dy * dy
    }

    fun getHsvWithLightness(lightness: Float): FloatArray? {
        if (hsvClone == null) hsvClone = hsv.clone()
        (hsvClone ?: return null)[0] = hsv[0]
        (hsvClone ?: return null)[1] = hsv[1]
        (hsvClone ?: return null)[2] = lightness
        return hsvClone
    }

    operator fun set(x: Float, y: Float, hsv: FloatArray) {
        this.x = x
        this.y = y
        this.hsv[0] = hsv[0]
        this.hsv[1] = hsv[1]
        this.hsv[2] = hsv[2]
        color = Color.HSVToColor(this.hsv)
    }

    init {
        set(x, y, hsv)
    }
}
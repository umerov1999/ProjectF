package de.maxr1998.modernpreferences.preferences.colorpicker.builder

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import kotlin.math.roundToInt

object PaintBuilder {
    fun newPaint(): PaintHolder {
        return PaintHolder()
    }

    fun createAlphaPatternShader(size: Int): Shader {
        var pSize = size
        pSize /= 2
        pSize = 8.coerceAtLeast(pSize * 2)
        return BitmapShader(
            createAlphaBackgroundPattern(pSize),
            Shader.TileMode.REPEAT,
            Shader.TileMode.REPEAT
        )
    }

    private fun createAlphaBackgroundPattern(size: Int): Bitmap {
        val alphaPatternPaint = newPaint().build()
        val bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bm)
        val s = (size / 2f).roundToInt()
        for (i in 0..1) for (j in 0..1) {
            if ((i + j) % 2 == 0) alphaPatternPaint.color = -0x1 else alphaPatternPaint.color =
                -0x2f2f30
            c.drawRect(
                (i * s).toFloat(),
                (j * s).toFloat(),
                ((i + 1) * s).toFloat(),
                ((j + 1) * s).toFloat(),
                alphaPatternPaint
            )
        }
        return bm
    }

    class PaintHolder {
        private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
        fun color(color: Int): PaintHolder {
            paint.color = color
            return this
        }

        fun antiAlias(flag: Boolean): PaintHolder {
            paint.isAntiAlias = flag
            return this
        }

        fun style(style: Paint.Style?): PaintHolder {
            paint.style = style
            return this
        }

        fun mode(mode: PorterDuff.Mode?): PaintHolder {
            paint.xfermode = PorterDuffXfermode(mode)
            return this
        }

        fun stroke(width: Float): PaintHolder {
            paint.strokeWidth = width
            return this
        }

        fun xPerMode(mode: PorterDuff.Mode?): PaintHolder {
            paint.xfermode = PorterDuffXfermode(mode)
            return this
        }

        fun shader(shader: Shader?): PaintHolder {
            paint.shader = shader
            return this
        }

        fun build(): Paint {
            return paint
        }

    }
}
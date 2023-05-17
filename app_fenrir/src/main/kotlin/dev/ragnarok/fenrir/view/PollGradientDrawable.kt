package dev.ragnarok.fenrir.view

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Shader
import android.graphics.drawable.Drawable
import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.model.Poll
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


class PollGradientDrawable(val pollBackground: Poll.PollBackground) :
    Drawable() {
    private val DEFAULT_RADIUS = 26f
    private val PATH = Path()
    private val paint =
        Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    override fun draw(canvas: Canvas) {
        try {
            val widthTmp = bounds.width()
            val heightTmp = bounds.height()
            if (widthTmp <= 0 || heightTmp <= 0) {
                return
            }
            val r = sqrt(
                heightTmp.toDouble().pow(2.0) +
                        widthTmp.toDouble().pow(2.0)
            ) / 2
            val centerX: Float = widthTmp.toFloat() / 2
            val centerY: Float = heightTmp.toFloat() / 2

            val sTmp = (sin(Math.toRadians(pollBackground.angle.toDouble())) * r).toFloat()
            val cTmp = (cos(Math.toRadians(pollBackground.angle.toDouble())) * r).toFloat()

            val startX = 0f.coerceAtLeast(widthTmp.toFloat().coerceAtMost(centerX - cTmp))
            val startY = heightTmp.toFloat().coerceAtMost(0f.coerceAtLeast(centerY - sTmp))

            val endX = 0f.coerceAtLeast(widthTmp.toFloat().coerceAtMost(centerX + cTmp))
            val endY = heightTmp.toFloat().coerceAtMost(0f.coerceAtLeast(centerY + sTmp))

            paint.shader = LinearGradient(
                startX,
                startY,
                endX,
                endY,
                pollBackground.getColors(),
                pollBackground.getPositions(),
                Shader.TileMode.CLAMP
            )
            PATH.reset()
            PATH.moveTo(0f, DEFAULT_RADIUS)
            PATH.arcTo(0f, 0f, 2 * DEFAULT_RADIUS, 2 * DEFAULT_RADIUS, 180f, 90f, false)
            PATH.lineTo(widthTmp - DEFAULT_RADIUS, 0f)
            PATH.arcTo(
                widthTmp - 2 * DEFAULT_RADIUS,
                0f,
                widthTmp.toFloat(),
                2 * DEFAULT_RADIUS,
                270f,
                90f,
                false
            )
            PATH.lineTo(widthTmp.toFloat(), heightTmp - DEFAULT_RADIUS)
            PATH.arcTo(
                widthTmp - 2 * DEFAULT_RADIUS,
                heightTmp - 2 * DEFAULT_RADIUS,
                widthTmp.toFloat(),
                heightTmp.toFloat(),
                0f,
                90f,
                false
            )
            PATH.lineTo(DEFAULT_RADIUS, heightTmp.toFloat())
            PATH.arcTo(
                0f,
                heightTmp - 2 * DEFAULT_RADIUS,
                2 * DEFAULT_RADIUS,
                heightTmp.toFloat(),
                90f,
                90f,
                false
            )
            PATH.lineTo(0f, DEFAULT_RADIUS)
            PATH.close()
            canvas.drawPath(PATH, paint)
        } catch (e: Exception) {
            if (Constants.IS_DEBUG) {
                e.printStackTrace()
            }
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        throw UnsupportedOperationException("setColorFilter unsupported!")
    }

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    init {
        paint.alpha = 220
        paint.style = Paint.Style.FILL
    }
}

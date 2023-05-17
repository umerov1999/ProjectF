package dev.ragnarok.fenrir.view

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import dev.ragnarok.fenrir.Constants

class ProfileCoverDrawable(private val bitmap: Bitmap, private val targetAlpha: Float) :
    Drawable() {
    private val paint =
        Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rBmp = Rect()

    private fun determineImageScale(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Double {
        val scalex = targetWidth.toDouble() / sourceWidth
        val scaley = targetHeight.toDouble() / sourceHeight
        return scalex.coerceAtMost(scaley)
    }

    private fun printBitmap(canvas: Canvas) {
        if (bounds.width() <= 0 || bounds.height() <= 0) {
            return
        }
        val pScale =
            determineImageScale(bounds.width(), bounds.height(), bitmap.width, bitmap.height)
        val px = bounds.width() * pScale
        val py = bounds.height() * pScale
        rBmp.left = ((bitmap.width - px) / 2).toInt()
        rBmp.top = ((bitmap.height - py) / 2).toInt()
        rBmp.right = bitmap.width - rBmp.left
        rBmp.bottom = bitmap.height - rBmp.top
        canvas.drawBitmap(bitmap, rBmp, bounds, paint)
    }

    override fun draw(canvas: Canvas) {
        try {
            paint.alpha = (targetAlpha * 255).toInt()
            printBitmap(canvas)
        } catch (e: Exception) {
            if (Constants.IS_DEBUG) {
                e.printStackTrace()
            }
        }
    }

    override fun setAlpha(alpha: Int) {
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

    companion object {
        // Only accessed from main thread.
        private const val FADE_DURATION = 400f //ms
        fun setBitmap(target: View, bitmap: Bitmap, targetAlpha: Float) {
            val drawable = ProfileCoverDrawable(bitmap, targetAlpha)
            drawable.callback = target
            target.background = drawable
        }
    }
}

package dev.ragnarok.filegallery.view.media

import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.widget.ImageView
import dev.ragnarok.filegallery.Constants
import dev.ragnarok.filegallery.model.PlayerCoverBackgroundSettings
import dev.ragnarok.filegallery.settings.Settings
import kotlin.math.abs
import kotlin.math.cos


class AudioPlayerCoverDrawable(private val bitmap: Bitmap) : Drawable() {
    private val startTimeMillis: Long = SystemClock.uptimeMillis()
    private val settings: PlayerCoverBackgroundSettings =
        Settings.get().main().getPlayerCoverBackgroundSettings()
    private var animatingFade = true
    private var pAlpha = 0xCC
    private var tmpRotation: Float = 0f
    private var targetSaturation = 1f
    private val paint =
        Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val rBmp = Rect()
    private val rDest = Rect()
    private var lastFrame: Long = 0
    private val colorMatrix = ColorMatrix()
    private lateinit var cFilter: ColorMatrixColorFilter

    private fun invalidateInternal() {
        if (callback != null) {
            invalidateSelf()
        }
    }

    private fun printBitmap(canvas: Canvas) {
        val pMaxDst = (bounds.width()
            .coerceAtLeast(bounds.height()))
        rDest.left = bounds.left - (pMaxDst - bounds.width()) / 2
        rDest.right = bounds.right + (pMaxDst - bounds.width()) / 2
        rDest.top = bounds.top - (pMaxDst - bounds.height()) / 2
        rDest.bottom = bounds.bottom + (pMaxDst - bounds.height()) / 2
        canvas.drawBitmap(bitmap, rBmp, rDest, paint)
    }

    private fun clamp(min: Float, max: Float, value: Float): Float {
        if (value < min) {
            return min
        } else if (value > max) {
            return max
        }
        return value
    }

    private fun updateTargetRotation(rotation: Float): Float {
        var ret = rotation
        if (settings.invert_rotation) {
            ret += settings.rotation_speed * clamp(
                0.2f,
                3f,
                (System.currentTimeMillis() - lastFrame) / 17f
            )
            if (ret > 360) {
                ret = 0f
            }
        } else {
            ret -= settings.rotation_speed * clamp(
                0.2f,
                3f,
                (System.currentTimeMillis() - lastFrame) / 17f
            )
            if (ret < -360) {
                ret = 0f
            }
        }
        return ret
    }

    private fun updateTargetSaturation() {
        if (settings.fade_saturation) {
            tmpRotation = updateTargetRotation(tmpRotation)
            targetSaturation =
                clamp(
                    0.2f,
                    1f,
                    abs(cos(Math.toRadians(tmpRotation.toDouble())).toFloat())
                )
            colorMatrix.setSaturation(targetSaturation)
            cFilter = ColorMatrixColorFilter(colorMatrix)
            paint.colorFilter = cFilter
        }
    }

    private fun doRotate(canvas: Canvas): Boolean {
        var invalidate = false
        if (!animatingFade) {
            if (settings.enabled_rotation && settings.fade_saturation && settings.rotation_speed > 0) {
                updateTargetSaturation()
                lastFrame = System.currentTimeMillis()
                invalidate = true
            }
            try {
                printBitmap(canvas)
            } catch (e: Exception) {
                if (Constants.IS_DEBUG) {
                    e.printStackTrace()
                }
            }
        } else {
            val normalized = (SystemClock.uptimeMillis() - startTimeMillis) / FADE_DURATION
            if (normalized >= 1f) {
                animatingFade = false
                try {
                    printBitmap(canvas)
                    if (settings.enabled_rotation && settings.fade_saturation && settings.rotation_speed > 0) {
                        invalidate = true
                    }
                } catch (e: Exception) {
                    if (Constants.IS_DEBUG) {
                        e.printStackTrace()
                    }
                }
            } else {
                // setAlpha will call invalidateSelf and drive the animation.
                val partialAlpha = (pAlpha * normalized).toInt()
                paint.alpha = partialAlpha
                try {
                    printBitmap(canvas)
                } catch (e: Exception) {
                    if (Constants.IS_DEBUG) {
                        e.printStackTrace()
                    }
                }
                paint.alpha = pAlpha
                invalidate = true
            }
        }
        return invalidate
    }

    override fun draw(canvas: Canvas) {
        val invalidate = doRotate(canvas)

        if (invalidate) {
            invalidateInternal()
        }
    }

    override fun setAlpha(alpha: Int) {
        this.pAlpha = alpha
        invalidateInternal()
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
        fun setBitmap(target: ImageView, bitmap: Bitmap) {
            val drawable = AudioPlayerCoverDrawable(bitmap)
            drawable.callback = target
            target.setImageDrawable(drawable)
        }
    }

    init {
        val pMin = bitmap.height.coerceAtMost(bitmap.width)
        rBmp.left = (bitmap.width - pMin) / 2
        rBmp.top = (bitmap.height - pMin) / 2
        rBmp.right = pMin - rBmp.left
        rBmp.bottom = pMin - rBmp.top
    }
}
package dev.ragnarok.fenrir.view.media

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils

class MediaActionDrawable : Drawable() {
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paint2 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paint3 = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val interpolator = DecelerateInterpolator()
    private var mColorFilter: ColorFilter? = null
    private var mTintList: ColorStateList? = null
    private var mTintMode: PorterDuff.Mode? = PorterDuff.Mode.SRC_IN
    private var mTintFilter: PorterDuffColorFilter? = null
    private var scale = 1.0f
    private var isMini = false
    private var transitionAnimationTime = 400.0f
    private var overrideAlpha = 1.0f
    private var previousIcon = 0
    private var nextIcon = 0
    private var transitionProgress = 1.0f
    private var savedTransitionProgress = 0f
    private var lastAnimationTime: Long = 0
    private var animatingTransition = false
    private var downloadRadOffset = 0f
    private var progress = 0f
    private var animatedDownloadProgress = 0f
    private var downloadProgressAnimationStart = 0f
    private var downloadProgressTime = 0f
    private var delegate: MediaActionDrawableDelegate? = null
    override fun setAlpha(alpha: Int) {}
    fun setOverrideAlpha(alpha: Float) {
        overrideAlpha = alpha
    }

    private fun updateTintFilter(): Boolean {
        if (mTintList == null || mTintMode == null) {
            val hadTintFilter = mTintFilter != null
            mTintFilter = null
            return hadTintFilter
        }
        val tintColor = mTintList?.getColorForState(state, Color.TRANSPARENT)
        tintColor ?: return false
        mTintFilter = PorterDuffColorFilter(tintColor, mTintMode!!)
        return true
    }

    private val colorFilterForDrawing: ColorFilter?
        get() = if (mColorFilter != null) mColorFilter else mTintFilter

    override fun getColorFilter(): ColorFilter? {
        return mColorFilter
    }

    /**
     * {@inheritDoc}
     */
    override fun setColorFilter(colorFilter: ColorFilter?) {
        mColorFilter = colorFilter
        invalidateSelf()
    }

    override fun setTint(@ColorInt tintColor: Int) {
        setTintList(ColorStateList.valueOf(tintColor))
    }

    override fun setTintList(tint: ColorStateList?) {
        mTintList = tint
        if (updateTintFilter()) {
            invalidateSelf()
        }
    }

    override fun setTintMode(tintMode: PorterDuff.Mode?) {
        mTintMode = tintMode
        if (updateTintFilter()) {
            invalidateSelf()
        }
    }

    override fun onStateChange(state: IntArray): Boolean {
        return updateTintFilter()
    }

    override fun isStateful(): Boolean {
        return mTintList != null && mTintList?.isStateful == true
    }

    fun setBackColor(value: Int) {
        backPaint.color = value or -0x1000000
    }

    fun setMini(value: Boolean) {
        isMini = value
        paint.strokeWidth = Utils.dpf2(if (isMini) 2f else 3f)
    }

    @Deprecated(
        "Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSPARENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int {
        return PixelFormat.TRANSPARENT
    }

    fun setDelegate(mediaActionDrawableDelegate: MediaActionDrawableDelegate?) {
        delegate = mediaActionDrawableDelegate
    }

    fun setIcon(icon: Int, animated: Boolean): Boolean {
        if (previousIcon == icon && nextIcon != icon) {
            previousIcon = nextIcon
            transitionProgress = 1.0f
        }
        if (animated) {
            if (previousIcon == icon || nextIcon == icon) {
                return false
            }
            transitionAnimationTime =
                if (previousIcon == ICON_PLAY && icon == ICON_PAUSE || previousIcon == ICON_PAUSE && icon == ICON_PLAY) {
                    300.0f
                } else {
                    220.0f
                }
            if (animatingTransition) {
                previousIcon = nextIcon
            }
            animatingTransition = true
            nextIcon = icon
            savedTransitionProgress = transitionProgress
            transitionProgress = 0.0f
        } else {
            if (previousIcon == icon) {
                return false
            }
            animatingTransition = false
            nextIcon = icon
            previousIcon = nextIcon
            savedTransitionProgress = transitionProgress
            transitionProgress = 1.0f
        }
        if (icon == ICON_CANCEL) {
            downloadRadOffset = 112f
            animatedDownloadProgress = 0.0f
            downloadProgressAnimationStart = 0.0f
            downloadProgressTime = 0.0f
        }
        invalidateSelf()
        return true
    }

    fun getCurrentIcon(): Int {
        return nextIcon
    }

    fun setProgress(value: Float, animated: Boolean) {
        if (!animated) {
            animatedDownloadProgress = value
            downloadProgressAnimationStart = value
        } else {
            if (animatedDownloadProgress > value) {
                animatedDownloadProgress = value
            }
            downloadProgressAnimationStart = animatedDownloadProgress
        }
        progress = value
        downloadProgressTime = 0f
        invalidateSelf()
    }

    private fun getCircleValue(value: Float): Float {
        var vl = value
        while (vl > 360f) {
            vl -= 360f
        }
        return vl
    }

    val progressAlpha: Float
        get() = 1.0f - transitionProgress

    fun getTransitionProgress(): Float {
        return if (animatingTransition) transitionProgress else 1.0f
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        scale = (right - left) / intrinsicWidth.toFloat()
        if (scale < 0.7f) {
            paint.strokeWidth = Utils.dp(2f).toFloat()
        }
    }

    override fun invalidateSelf() {
        super.invalidateSelf()
        delegate?.invalidate()
    }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        paint.shader = null
        paint2.shader = null
        paint3.shader = null
        val rs = colorFilterForDrawing
        paint.colorFilter = rs
        paint2.colorFilter = rs
        paint3.colorFilter = rs
        textPaint.colorFilter = rs
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        var saveCount = 0
        if (nextIcon == ICON_NONE) {
            if (previousIcon != ICON_CANCEL) {
                saveCount = canvas.save()
                val progress = 1.0f - transitionProgress
                canvas.scale(progress, progress, cx.toFloat(), cy.toFloat())
            }
        } else if ((nextIcon == ICON_EMPTY) && previousIcon == ICON_NONE) {
            saveCount = canvas.save()
            canvas.scale(transitionProgress, transitionProgress, cx.toFloat(), cy.toFloat())
        }
        if (previousIcon == ICON_CANCEL || previousIcon == ICON_NONE && nextIcon == ICON_CANCEL) {
            val d: Float
            val rotation: Float
            var iconScale = 1.0f
            var iconScaleX = 0f
            var iconScaleY = 0f
            val alpha: Int
            when (nextIcon) {
                ICON_PLAY, ICON_PAUSE -> {
                    val backProgress: Float
                    val progress: Float = transitionProgress
                    backProgress = 1.0f - progress
                    rotation = 45 * progress
                    d = Utils.dp(7f) * backProgress * scale
                    alpha = (255 * 1.0f.coerceAtMost(backProgress * 2.0f)).toInt()
                }

                ICON_NONE -> {
                    val progress = transitionProgress
                    val backProgress = 1.0f - progress
                    d = Utils.dp(7f) * scale
                    alpha = (255 * backProgress).toInt()
                    rotation = 45 * progress
                    iconScale = 1.0f
                    iconScaleX = bounds.centerX().toFloat()
                    iconScaleY = bounds.centerY().toFloat()
                }

                ICON_CANCEL -> {
                    val progress = transitionProgress
                    val backProgress = 1.0f - progress
                    if (previousIcon == ICON_NONE) {
                        rotation = 0f
                        iconScale = progress
                    } else {
                        rotation = 45 * backProgress
                        iconScale = 1.0f
                    }
                    d = Utils.dp(7f) * scale
                    alpha = (255 * progress).toInt()
                    iconScaleX = bounds.centerX().toFloat()
                    iconScaleY = bounds.centerY().toFloat()
                }

                else -> {
                    rotation = 0f
                    d = Utils.dp(7f) * scale
                    alpha = 255
                }
            }
            if (iconScale != 1.0f) {
                canvas.save()
                canvas.scale(iconScale, iconScale, iconScaleX, iconScaleY)
            }
            if (rotation != 0f) {
                canvas.save()
                canvas.rotate(rotation, cx.toFloat(), cy.toFloat())
            }
            if (alpha != 0) {
                paint.alpha = (alpha * overrideAlpha).toInt()
                canvas.drawLine(cx - d, cy - d, cx + d, cy + d, paint)
                canvas.drawLine(cx + d, cy - d, cx - d, cy + d, paint)
            }
            if (rotation != 0f) {
                canvas.restore()
            }
            if ((previousIcon == ICON_CANCEL || previousIcon == ICON_NONE && (nextIcon == ICON_CANCEL)) && alpha != 0) {
                val rad = 4f.coerceAtLeast(360 * animatedDownloadProgress)
                val diff = Utils.dpf2(if (isMini) 2f else 4f)
                rect[(bounds.left + diff), (bounds.top + diff), (bounds.right - diff)] =
                    (bounds.bottom - diff)
                if (previousIcon == ICON_NONE && nextIcon == ICON_CANCEL) {
                    paint.alpha = (alpha * 0.15f * overrideAlpha).toInt()
                    canvas.drawArc(rect, 0f, 360f, false, paint)
                    paint.alpha = alpha
                }
                canvas.drawArc(rect, downloadRadOffset, rad, false, paint)
            }
            if (iconScale != 1.0f) {
                canvas.restore()
            }
        } else if (previousIcon == ICON_EMPTY || nextIcon == ICON_EMPTY) {
            val alpha: Int = if (nextIcon == ICON_NONE) {
                val progress = transitionProgress
                val backProgress = 1.0f - progress
                (255 * backProgress).toInt()
            } else {
                255
            }
            if (alpha != 0) {
                paint.alpha = (alpha * overrideAlpha).toInt()
                val rad = 4f.coerceAtLeast(360 * animatedDownloadProgress)
                val diff = Utils.dpf2(if (isMini) 2f else 4f)
                rect[(bounds.left + diff), (bounds.top + diff), (bounds.right - diff)] =
                    (bounds.bottom - diff)
                canvas.drawArc(rect, downloadRadOffset, rad, false, paint)
            }
        }
        val drawableScale: Float = when (previousIcon) {
            nextIcon -> {
                1.0f
            }

            ICON_NONE -> {
                transitionProgress
            }

            else -> {
                1.0f.coerceAtMost(transitionProgress / 0.5f)
            }
        }
        if (previousIcon == ICON_PLAY || previousIcon == ICON_PAUSE || nextIcon == ICON_PLAY || nextIcon == ICON_PAUSE) {
            val p: Float =
                if (previousIcon == ICON_PLAY && nextIcon == ICON_PAUSE || previousIcon == ICON_PAUSE && nextIcon == ICON_PLAY) {
                    if (animatingTransition) {
                        if (nextIcon == ICON_PLAY) {
                            1.0f - transitionProgress
                        } else {
                            transitionProgress
                        }
                    } else {
                        if (nextIcon == ICON_PAUSE) 1.0f else 0.0f
                    }
                } else {
                    if (previousIcon == ICON_PAUSE) 1.0f else 0.0f
                }
            if (nextIcon != ICON_PLAY && nextIcon != ICON_PAUSE || previousIcon != ICON_PLAY && previousIcon != ICON_PAUSE) {
                if (nextIcon == ICON_NONE) {
                    paint2.alpha = (255 * (1.0f - transitionProgress)).toInt()
                } else {
                    paint2.alpha = (transitionProgress * 255).toInt()
                }
            } else {
                paint2.alpha = 255
            }
            canvas.save()
            canvas.translate(
                bounds.centerX() + Utils.dp(1f) * (1.0f - p),
                bounds.centerY().toFloat()
            )
            var ms = 500.0f * p
            var rotation: Float = if (previousIcon == ICON_PAUSE) 90f else 0f
            if (previousIcon == ICON_PLAY && nextIcon == ICON_PAUSE) {
                rotation = when {
                    ms < 384 -> {
                        95 * CubicBezierInterpolator.EASE_BOTH.getInterpolation(ms / 384)
                    }

                    ms < 484 -> {
                        95 - 5 * CubicBezierInterpolator.EASE_BOTH.getInterpolation((ms - 384) / 100.0f)
                    }

                    else -> {
                        90f
                    }
                }
                ms += 100f
            } else if (previousIcon == ICON_PAUSE && nextIcon == ICON_PLAY) {
                if (ms < 100) {
                    rotation = -5 * CubicBezierInterpolator.EASE_BOTH.getInterpolation(ms / 100.0f)
                } else if (ms < 484) {
                    rotation =
                        -5 + 95 * CubicBezierInterpolator.EASE_BOTH.getInterpolation((ms - 100) / 384)
                }
            }
            canvas.rotate(rotation)
            if (previousIcon != ICON_PLAY && previousIcon != ICON_PAUSE) {
                canvas.scale(drawableScale, drawableScale)
            }
            CurrentTheme.playPauseAnimator.draw(canvas, paint2, ms)
            canvas.scale(1.0f, -1.0f)
            CurrentTheme.playPauseAnimator.draw(canvas, paint2, ms)
            canvas.restore()
        }
        val newTime = System.currentTimeMillis()
        var dt = newTime - lastAnimationTime
        if (dt > 17) {
            dt = 17
        }
        lastAnimationTime = newTime
        if (previousIcon == ICON_CANCEL || previousIcon == ICON_EMPTY) {
            downloadRadOffset += 360 * dt / 2500.0f
            downloadRadOffset = getCircleValue(downloadRadOffset)
            val progressDiff = progress - downloadProgressAnimationStart
            if (progressDiff > 0) {
                downloadProgressTime += dt.toFloat()
                if (downloadProgressTime >= 200.0f) {
                    animatedDownloadProgress = progress
                    downloadProgressAnimationStart = progress
                    downloadProgressTime = 0f
                } else {
                    animatedDownloadProgress =
                        downloadProgressAnimationStart + progressDiff * interpolator.getInterpolation(
                            downloadProgressTime / 200.0f
                        )
                }
            }
            invalidateSelf()
        }
        if (animatingTransition) {
            if (transitionProgress < 1.0f) {
                transitionProgress += dt / transitionAnimationTime
                if (transitionProgress >= 1.0f) {
                    previousIcon = nextIcon
                    transitionProgress = 1.0f
                    animatingTransition = false
                }
                invalidateSelf()
            }
        }
        if (saveCount >= 1) {
            canvas.restoreToCount(saveCount)
        }
    }

    override fun getIntrinsicWidth(): Int {
        return Utils.dp(48f)
    }

    override fun getIntrinsicHeight(): Int {
        return Utils.dp(48f)
    }

    override fun getMinimumWidth(): Int {
        return Utils.dp(48f)
    }

    override fun getMinimumHeight(): Int {
        return Utils.dp(48f)
    }

    interface MediaActionDrawableDelegate {
        fun invalidate()
    }

    companion object {
        const val ICON_PLAY = 0
        const val ICON_PAUSE = 1
        const val ICON_CANCEL = 2
        const val ICON_NONE = 3
        const val ICON_EMPTY = 4
    }

    init {
        paint.color = -0x1
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = Utils.dp(3f).toFloat()
        paint.style = Paint.Style.STROKE
        paint3.color = -0x1
        textPaint.textSize = Utils.dp(13f).toFloat()
        textPaint.color = -0x1
        paint2.color = -0x1
    }
}

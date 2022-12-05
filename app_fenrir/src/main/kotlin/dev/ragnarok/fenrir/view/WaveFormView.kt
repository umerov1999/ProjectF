package dev.ragnarok.fenrir.view

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Property
import android.view.View
import androidx.annotation.ColorInt
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.util.Exestime
import java.lang.ref.WeakReference

class WaveFormView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    View(context, attrs) {
    companion object {
        private val PAINT = Paint(
            Paint.FILTER_BITMAP_FLAG
                    or Paint.DITHER_FLAG
                    or Paint.ANTI_ALIAS_FLAG
        )
        private val PROGRESS_PROPERTY: Property<WaveFormView, Float> =
            object : Property<WaveFormView, Float>(
                Float::class.java, "displayed-precentage"
            ) {
                override fun get(view: WaveFormView): Float {
                    return view.mDisplayedProgress
                }

                override fun set(view: WaveFormView, value: Float) {
                    view.mDisplayedProgress = value
                    view.invalidate()
                }
            }

        internal fun calculateMaxValue(values: ByteArray): Byte {
            var max = if (values.isNotEmpty()) values[0] else 0
            for (value in values) if (value > max) max = value
            return max
        }

        init {
            PAINT.style = Paint.Style.STROKE
            PAINT.strokeCap = Paint.Cap.ROUND
            PAINT.strokeJoin = Paint.Join.ROUND
        }
    }

    @ColorInt
    private var mActiveColor = 0

    @ColorInt
    private var mNoactiveColor = 0
    private var mSectionCount = 64
    private var mWaveForm = ByteArray(0)
    private var mMaxValue = 50f
    private var mCurrentActiveProgress = 0f
    private var mDisplayedProgress = 0f
    private var mAnimator = WeakReference<ObjectAnimator?>(null)
    private fun init(context: Context, attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.WaveFormView, 0, 0)
        try {
            mActiveColor = a.getColor(R.styleable.WaveFormView_waveform_active_color, Color.BLUE)
            mNoactiveColor = a.getInt(R.styleable.WaveFormView_waveform_noactive_color, Color.GRAY)
        } finally {
            a.recycle()
        }
        if (isInEditMode) {
            setCurrentActiveProgress(0.4f)
            setWaveForm(
                byteArrayOf(
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 6, 6, 9, 10, 13, 9, 8, 12, 13,
                    1, 5, 7, 18, 1, 10, 16, 6, 12, 31, 8, 15, 5, 13, 11, 11, 13, 10, 13, 9, 23, 17,
                    8, 7, 5, 7, 6, 3, 6, 2, 8, 9, 9, 1, 2, 29, 16, 8, 10, 10, 6, 3, 1, 1, 3, 2, 5,
                    9, 11, 13, 14, 7, 3, 6, 2, 3, 5, 5, 9, 10, 11, 11, 2, 0, 1, 2, 6, 7, 8, 5, 2,
                    3, 1, 1, 1, 3, 1, 5, 4, 1, 1, 3, 6, 8, 4
                )
            )
        }
    }

    private fun setCurrentActiveProgress(progress: Float) {
        if (mCurrentActiveProgress == progress) {
            return
        }
        mCurrentActiveProgress = progress
        mDisplayedProgress = progress
        releaseAnimation()
        invalidate()
    }

    private fun releaseAnimation() {
        val animator = mAnimator.get()
        if (animator != null) {
            animator.cancel()
            mAnimator = WeakReference(null)
        }
    }

    private fun setCurrentActiveProgressSmoothly(progress: Float) {
        if (mCurrentActiveProgress == progress) {
            return
        }
        mCurrentActiveProgress = progress
        val animator = ObjectAnimator.ofFloat(this, PROGRESS_PROPERTY, progress)
        mAnimator = WeakReference(animator)
        animator.duration = 900
        //animator.setInterpolator(new AccelerateInterpolator(1.75f));
        animator.start()
    }

    fun setCurrentActiveProgress(progress: Float, anim: Boolean) {
        if (anim) {
            setCurrentActiveProgressSmoothly(progress)
        } else {
            setCurrentActiveProgress(progress)
        }
    }

    fun setSectionCount(sectionCount: Int) {
        mSectionCount = sectionCount
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = calculateSectionWidth()
        PAINT.strokeWidth = size * 2f
        var offset = 0f
        for (i in mWaveForm.indices) {
            val active = i.toFloat() / mWaveForm.size.toFloat() <= mDisplayedProgress
            @ColorInt val color = if (active) mActiveColor else mNoactiveColor
            PAINT.color = color
            val value = mWaveForm[i]
            val pxHeight = height.toFloat() * (value.toFloat() / mMaxValue)
            val verticalPadding = (height.toFloat() - pxHeight) / 2f
            val startX = offset + size
            canvas.drawLine(startX, verticalPadding, startX, pxHeight + verticalPadding, PAINT)
            offset += size * 3
        }
    }

    private fun calculateSectionWidth(): Float {
        val count = (mWaveForm.size * 3.03 - 1).toInt()
        return width.toFloat() / count.toFloat()
    }

    fun setWaveForm(waveForm: ByteArray) {
        val start = System.currentTimeMillis()
        mWaveForm = ByteArray(waveForm.size)
        for (i in waveForm.indices) {
            mWaveForm[i] = (waveForm[i] + 1).toByte()
        }
        cut()
        Exestime.log("setWaveForm", start, "count: " + waveForm.size)
        invalidateMaxValue()
        invalidate()
    }

    private fun cut() {
        val newValues = ByteArray(mSectionCount)
        for (i in 0 until mSectionCount) {
            newValues[i] = getValueAt(mWaveForm, i.toFloat() / mSectionCount.toFloat())
        }
        mWaveForm = newValues
    }

    private fun getValueAt(values: ByteArray?, coef: Float): Byte {
        if (values == null) {
            return 0
        }
        val index = (values.size.toFloat() * coef).toInt()
        return values[index]
    }

    private fun invalidateMaxValue() {
        val newMaxValue = calculateMaxValue(mWaveForm)
        if (newMaxValue > mMaxValue) {
            mMaxValue = newMaxValue.toFloat()
        }
    }

    fun setActiveColor(@ColorInt activeColor: Int) {
        mActiveColor = activeColor
    }

    fun setNoactiveColor(@ColorInt noactiveColor: Int) {
        mNoactiveColor = noactiveColor
    }

    init {
        init(context, attrs)
    }
}
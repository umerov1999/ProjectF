package me.minetsh.imaging.view

import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.withStyledAttributes
import androidx.core.graphics.withSave
import com.google.android.material.radiobutton.MaterialRadioButton
import me.minetsh.imaging.R
import kotlin.math.min

/**
 * Created by felix on 2017/12/1 下午2:50.
 */
class IMGColorRadio : MaterialRadioButton, AnimatorUpdateListener {
    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var mColor = Color.WHITE
    private var mStrokeColor = Color.WHITE
    private var mRadiusRatio = 0f
    private var mAnimator: ValueAnimator? = null

    constructor(context: Context) : super(context) {
        initialize(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initialize(context, attrs)
    }

    private fun initialize(context: Context, attrs: AttributeSet?) {
        context.withStyledAttributes(attrs, R.styleable.IMGColorRadio) {
            mColor = getColor(R.styleable.IMGColorRadio_image_color, Color.WHITE)
            mStrokeColor = getColor(R.styleable.IMGColorRadio_image_stroke_color, Color.WHITE)
        }
        buttonDrawable = null
        mPaint.color = mColor
        mPaint.strokeWidth = 5f
    }

    private val animator: ValueAnimator?
        get() {
            if (mAnimator == null) {
                mAnimator = ValueAnimator.ofFloat(0f, 1f)
                mAnimator?.addUpdateListener(this)
                mAnimator?.setDuration(200)
                mAnimator?.interpolator = AccelerateDecelerateInterpolator()
            }
            return mAnimator
        }
    var color: Int
        get() = mColor
        set(color) {
            mColor = color
            mPaint.color = mColor
        }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val hw = width / 2f
        val hh = height / 2f
        val radius = min(hw, hh)
        canvas.withSave {
            mPaint.color = mColor
            mPaint.style = Paint.Style.FILL
            drawCircle(hw, hh, getBallRadius(radius), mPaint)
            mPaint.color = mStrokeColor
            mPaint.style = Paint.Style.STROKE
            drawCircle(hw, hh, getRingRadius(radius), mPaint)
        }
    }

    private fun getBallRadius(radius: Float): Float {
        return radius * ((RADIUS_BALL - RADIUS_BASE) * mRadiusRatio + RADIUS_BASE)
    }

    private fun getRingRadius(radius: Float): Float {
        return radius * ((RADIUS_RING - RADIUS_BASE) * mRadiusRatio + RADIUS_BASE)
    }

    override fun setChecked(checked: Boolean) {
        val isChanged = checked != isChecked
        super.setChecked(checked)
        if (isChanged) {
            val animator = animator
            if (checked) {
                animator?.start()
            } else {
                animator?.reverse()
            }
        }
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        mRadiusRatio = animation.animatedValue as Float
        invalidate()
    }

    companion object {
        private const val TAG = "IMGColorRadio"
        private const val RADIUS_BASE = 0.6f
        private const val RADIUS_RING = 0.9f
        private const val RADIUS_BALL = 0.72f
    }
}

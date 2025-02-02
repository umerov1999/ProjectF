package dev.ragnarok.fenrir.view

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import com.google.android.material.imageview.ShapeableImageView
import dev.ragnarok.fenrir.R

/**
 * Maintains an aspect ratio based on either width or height. Disabled by default.
 */
class AspectRatioImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ShapeableImageView(context, attrs) {
    private var aspectRatio: Float = 1f
    private var aspectRatioEnabled: Boolean = false
    private var dominantMeasurement: Int = MEASUREMENT_WIDTH
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (!aspectRatioEnabled) return
        val newWidth: Int
        val newHeight: Int
        when (dominantMeasurement) {
            MEASUREMENT_WIDTH -> {
                newWidth = measuredWidth
                newHeight = (newWidth / aspectRatio).toInt()
            }

            MEASUREMENT_HEIGHT -> {
                newHeight = measuredHeight
                newWidth = (newHeight / aspectRatio).toInt()
            }

            else -> throw IllegalStateException("Unknown measurement with ID $dominantMeasurement")
        }
        setMeasuredDimension(newWidth, newHeight)
    }

    /**
     * Get the aspect ratio for this image view.
     */
    fun getAspectRatio(): Float {
        return aspectRatio
    }

    /**
     * Set the aspect ratio for this image view. This will update the view instantly.
     */
    fun setAspectRatio(aspectRatio: Float) {
        this.aspectRatio = aspectRatio
        if (aspectRatioEnabled) {
            requestLayout()
        }
    }

    /**
     * Set the aspect ratio for this image view. This will update the view instantly.
     */
    fun setAspectRatio(w: Int, h: Int) {
        aspectRatio = h.toFloat() / w.toFloat()
        if (aspectRatioEnabled) {
            requestLayout()
        }
    }

    /**
     * Get whether or not forcing the aspect ratio is enabled.
     */
    fun getAspectRatioEnabled(): Boolean {
        return aspectRatioEnabled
    }

    /**
     * set whether or not forcing the aspect ratio is enabled. This will re-layout the view.
     */
    fun setAspectRatioEnabled(aspectRatioEnabled: Boolean) {
        this.aspectRatioEnabled = aspectRatioEnabled
        requestLayout()
    }

    /**
     * Get the dominant measurement for the aspect ratio.
     */
    fun getDominantMeasurement(): Int {
        return dominantMeasurement
    }

    /**
     * Set the dominant measurement for the aspect ratio.
     *
     * @see .MEASUREMENT_WIDTH
     *
     * @see .MEASUREMENT_HEIGHT
     */
    fun setDominantMeasurement(dominantMeasurement: Int) {
        require(!(dominantMeasurement != MEASUREMENT_HEIGHT && dominantMeasurement != MEASUREMENT_WIDTH)) { "Invalid measurement type." }
        this.dominantMeasurement = dominantMeasurement
        requestLayout()
    }

    companion object {
        // NOTE: These must be kept in sync with the AspectRatioImageView attributes in attrs.xml.
        const val MEASUREMENT_WIDTH = 0
        const val MEASUREMENT_HEIGHT = 1
        private const val DEFAULT_ASPECT_RATIO_W = 1
        private const val DEFAULT_ASPECT_RATIO_H = 1
        private const val DEFAULT_ASPECT_RATIO_ENABLED = false
        private const val DEFAULT_DOMINANT_MEASUREMENT = MEASUREMENT_WIDTH
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.AspectRatioImageView) {
            val aspectRatioW =
                getInt(R.styleable.AspectRatioImageView_aspectRatioW, DEFAULT_ASPECT_RATIO_W)
            val aspectRatioH =
                getInt(R.styleable.AspectRatioImageView_aspectRatioH, DEFAULT_ASPECT_RATIO_H)
            aspectRatio = aspectRatioW.toFloat() / aspectRatioH.toFloat()
            aspectRatioEnabled = getBoolean(
                R.styleable.AspectRatioImageView_aspectRatioEnabled,
                DEFAULT_ASPECT_RATIO_ENABLED
            )
            dominantMeasurement = getInt(
                R.styleable.AspectRatioImageView_dominantMeasurement,
                DEFAULT_DOMINANT_MEASUREMENT
            )
        }
    }
}

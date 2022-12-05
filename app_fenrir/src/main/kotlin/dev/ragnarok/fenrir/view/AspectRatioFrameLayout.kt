package dev.ragnarok.fenrir.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.IntDef
import dev.ragnarok.fenrir.R
import kotlin.math.abs

/** A [FrameLayout] that resizes itself to match a specified aspect ratio.  */
class AspectRatioFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? =  /* attrs= */null
) : FrameLayout(context, attrs) {

    /**
     * Resize modes for [AspectRatioFrameLayout]. One of [.RESIZE_MODE_FIT], [ ][.RESIZE_MODE_FIXED_WIDTH], [.RESIZE_MODE_FIXED_HEIGHT], [.RESIZE_MODE_FILL] or
     * [.RESIZE_MODE_ZOOM].
     */
    @MustBeDocumented
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.TYPE)
    @IntDef(
        RESIZE_MODE_FIT,
        RESIZE_MODE_FIXED_WIDTH,
        RESIZE_MODE_FIXED_HEIGHT,
        RESIZE_MODE_FILL,
        RESIZE_MODE_ZOOM
    )
    annotation class ResizeMode

    private var frameAspectRatio = 0f
    private var resizeMode: @ResizeMode Int

    /**
     * Sets the aspect ratio that this view should satisfy.
     *
     * @param widthHeightRatio The width to height ratio.
     */
    fun setAspectRatio(widthHeightRatio: Float) {
        if (frameAspectRatio != widthHeightRatio) {
            frameAspectRatio = widthHeightRatio
            requestLayout()
        }
    }

    /** Returns the [ResizeMode].  */
    fun getResizeMode(): @ResizeMode Int {
        return resizeMode
    }

    /**
     * Sets the [ResizeMode]
     *
     * @param resizeMode The [ResizeMode].
     */
    fun setResizeMode(resizeMode: @ResizeMode Int) {
        if (this.resizeMode != resizeMode) {
            this.resizeMode = resizeMode
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (frameAspectRatio <= 0) {
            // Aspect ratio not set.
            return
        }
        var width = measuredWidth
        var height = measuredHeight
        val viewAspectRatio = width.toFloat() / height
        val aspectDeformation = frameAspectRatio / viewAspectRatio - 1
        if (abs(aspectDeformation) <= MAX_ASPECT_RATIO_DEFORMATION_FRACTION) {
            // We're within the allowed tolerance.
            return
        }
        when (resizeMode) {
            RESIZE_MODE_FIXED_WIDTH -> height = (width / frameAspectRatio).toInt()
            RESIZE_MODE_FIXED_HEIGHT -> width = (height * frameAspectRatio).toInt()
            RESIZE_MODE_ZOOM -> if (aspectDeformation > 0) {
                width = (height * frameAspectRatio).toInt()
            } else {
                height = (width / frameAspectRatio).toInt()
            }
            RESIZE_MODE_FIT -> if (aspectDeformation > 0) {
                height = (width / frameAspectRatio).toInt()
            } else {
                width = (height * frameAspectRatio).toInt()
            }
            RESIZE_MODE_FILL -> {}
            else -> {}
        }
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
    }

    companion object {
        /** Either the width or height is decreased to obtain the desired aspect ratio.  */
        const val RESIZE_MODE_FIT = 0

        /**
         * The width is fixed and the height is increased or decreased to obtain the desired aspect ratio.
         */
        const val RESIZE_MODE_FIXED_WIDTH = 1

        /**
         * The height is fixed and the width is increased or decreased to obtain the desired aspect ratio.
         */
        const val RESIZE_MODE_FIXED_HEIGHT = 2

        /** The specified aspect ratio is ignored.  */
        const val RESIZE_MODE_FILL = 3

        /** Either the width or height is increased to obtain the desired aspect ratio.  */
        const val RESIZE_MODE_ZOOM = 4

        /**
         * The [FrameLayout] will not resize itself if the fractional difference between its natural
         * aspect ratio and the requested aspect ratio falls below this threshold.
         *
         *
         * This tolerance allows the view to occupy the whole of the screen when the requested aspect
         * ratio is very close, but not exactly equal to, the aspect ratio of the screen. This may reduce
         * the number of view layers that need to be composited by the underlying system, which can help
         * to reduce power consumption.
         */
        private const val MAX_ASPECT_RATIO_DEFORMATION_FRACTION = 0.01f
    }

    init {
        resizeMode = RESIZE_MODE_FIT
        if (attrs != null) {
            val a = context
                .theme
                .obtainStyledAttributes(attrs, R.styleable.AspectRatioFrameLayout, 0, 0)
            resizeMode = try {
                if (a.getBoolean(R.styleable.AspectRatioFrameLayout_useAspect, false)) {
                    frameAspectRatio = a.getInt(R.styleable.AspectRatioFrameLayout_aspectWidth, 1)
                        .toFloat() / a.getInt(R.styleable.AspectRatioFrameLayout_aspectHeight, 1)
                        .toFloat()
                }
                a.getInt(
                    R.styleable.AspectRatioFrameLayout_resize_mode,
                    RESIZE_MODE_FIT
                )
            } finally {
                a.recycle()
            }
        }
    }
}
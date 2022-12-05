package dev.ragnarok.filegallery.view

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView
import androidx.annotation.IntDef

class ExpandableSurfaceView(context: Context?, attrs: AttributeSet?) : SurfaceView(context, attrs) {
    @MustBeDocumented
    @Retention(AnnotationRetention.SOURCE)
    @Target(AnnotationTarget.TYPE)
    @IntDef(
        RESIZE_MODE_FIT,
        RESIZE_MODE_ZOOM
    )
    annotation class ResizeMode

    private var resizeMode: Int = RESIZE_MODE_FIT
    private var videoAspectRatio = 0.0f
    private var scaleXF = 1.0f
    private var scaleYF = 1.0f
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (videoAspectRatio == 0.0f) {
            return
        }
        var width = MeasureSpec.getSize(widthMeasureSpec)
        // Use maxHeight only on non-fit resize mode and in vertical videos
        var height = MeasureSpec.getSize(heightMeasureSpec)
        if (height == 0) {
            return
        }
        val viewAspectRatio = width / height.toFloat()
        val aspectDeformation = videoAspectRatio / viewAspectRatio - 1
        scaleXF = 1.0f
        scaleYF = 1.0f
        if (resizeMode == RESIZE_MODE_FIT) {
            if (aspectDeformation > 0) {
                height = (width / videoAspectRatio).toInt()
            } else {
                width = (height * videoAspectRatio).toInt()
            }
        } else if (resizeMode == RESIZE_MODE_ZOOM) {
            if (aspectDeformation < 0) {
                scaleYF = viewAspectRatio / videoAspectRatio
            } else {
                scaleXF = videoAspectRatio / viewAspectRatio
            }
        }
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        )
    }

    /**
     * Scale view only in [.onLayout] to make transition for ZOOM mode as smooth as possible.
     */
    override fun onLayout(
        changed: Boolean,
        left: Int, top: Int, right: Int, bottom: Int
    ) {
        scaleX = scaleXF
        scaleY = scaleYF
    }

    fun setResizeMode(newResizeMode: @ResizeMode Int) {
        if (resizeMode == newResizeMode) {
            return
        }
        resizeMode = newResizeMode
        requestLayout()
    }

    fun getResizeMode(): @ResizeMode Int {
        return resizeMode
    }

    fun setAspectRatio(width: Int, height: Int) {
        if (width <= 0 || height <= 0) {
            return
        }
        val tmpAspectRatio = width.toFloat() / height.toFloat()
        if (videoAspectRatio == tmpAspectRatio) {
            return
        }
        videoAspectRatio = tmpAspectRatio
        requestLayout()
    }

    fun setAspectRatio(aspectRatio: Float) {
        if (videoAspectRatio == aspectRatio) {
            return
        }
        videoAspectRatio = aspectRatio
        requestLayout()
    }

    companion object {
        const val RESIZE_MODE_FIT = 0
        const val RESIZE_MODE_ZOOM = 1
    }
}
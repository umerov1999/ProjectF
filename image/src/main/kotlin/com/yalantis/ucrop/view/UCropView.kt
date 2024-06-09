package com.yalantis.ucrop.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.yalantis.ucrop.callback.CropBoundsChangeListener
import com.yalantis.ucrop.callback.OverlayViewChangeListener
import me.minetsh.imaging.R

class UCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    val overlayView: OverlayView
    var cropImageView: GestureCropImageView
        private set

    init {
        LayoutInflater.from(context).inflate(R.layout.ucrop_view, this, true)
        cropImageView = findViewById(R.id.image_view_crop)
        overlayView = findViewById(R.id.view_overlay)
        @SuppressLint("CustomViewStyleable") val a =
            context.obtainStyledAttributes(attrs, R.styleable.ucrop_UCropView)
        overlayView.processStyledAttributes(a)
        cropImageView.processStyledAttributes(a)
        a.recycle()
        setListenersToViews()
    }

    private fun setListenersToViews() {
        cropImageView.cropBoundsChangeListener =
            CropBoundsChangeListener {
                overlayView.setTargetAspectRatio(it)
            }
        overlayView.overlayViewChangeListener = OverlayViewChangeListener {
            cropImageView.setCropRect(
                it
            )
        }
    }

    override fun shouldDelayChildPressedState(): Boolean {
        return false
    }

    /**
     * Method for reset state for UCropImageView such as rotation, scale, translation.
     * Be careful: this method recreate UCropImageView instance and reattach it to layout.
     */
    fun resetCropImageView() {
        removeView(cropImageView)
        cropImageView = GestureCropImageView(context)
        setListenersToViews()
        cropImageView.setCropRect(overlayView.cropViewRect)
        addView(cropImageView, 0)
    }
}
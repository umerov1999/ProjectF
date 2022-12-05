package dev.ragnarok.fenrir.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.MaterialShapeUtils
import dev.ragnarok.fenrir.R

class BottomShadowView : FrameLayout {
    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    ) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        outlineProvider = ViewOutlineProvider.BOUNDS
        val a = getContext().obtainStyledAttributes(attrs, R.styleable.BottomShadowView)
        ViewCompat.setBackground(
            this,
            a.getDrawable(R.styleable.BottomShadowView_android_background)
        )
        if (background is ColorDrawable) {
            val background = background as ColorDrawable
            val materialShapeDrawable = MaterialShapeDrawable()
            materialShapeDrawable.fillColor = ColorStateList.valueOf(background.color)
            materialShapeDrawable.initializeElevationOverlay(context)
            ViewCompat.setBackground(this, materialShapeDrawable)
        }
        elevation = a.getDimension(R.styleable.BottomShadowView_elevation, 0f)
        a.recycle()
    }

    override fun setElevation(elevation: Float) {
        super.setElevation(elevation)
        MaterialShapeUtils.setElevation(this, elevation)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        MaterialShapeUtils.setParentAbsoluteElevation(this)
    }
}
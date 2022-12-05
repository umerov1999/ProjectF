package dev.ragnarok.filegallery.view.media

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.FloatingActionButton

open class MaterialPlayPauseFab : FloatingActionButton {
    private val mDrawable: MediaActionDrawable

    constructor(context: Context) : super(context) {
        mDrawable = MediaActionDrawable()
        this.setImageDrawable(mDrawable)
        scaleType = ScaleType.FIT_XY
    }

    constructor(context: Context, attrs: AttributeSet?) : super(
        context, attrs
    ) {
        mDrawable = MediaActionDrawable()
        this.setImageDrawable(mDrawable)
        scaleType = ScaleType.FIT_XY
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context, attrs, defStyleAttr
    ) {
        mDrawable = MediaActionDrawable()
        this.setImageDrawable(mDrawable)
        scaleType = ScaleType.FIT_XY
    }

    fun setIcon(icon: Int, anim: Boolean) {
        mDrawable.setIcon(icon, anim)
    }

    fun setProgress(percent: Int, anim: Boolean) {
        mDrawable.setProgress(percent.toFloat(), anim)
    }
}
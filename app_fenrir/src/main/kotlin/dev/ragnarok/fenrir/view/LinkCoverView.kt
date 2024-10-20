package dev.ragnarok.fenrir.view

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.imageview.ShapeableImageView

class LinkCoverView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ShapeableImageView(context, attrs) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (measuredWidth > 0 && measuredHeight > 0) {
            setMeasuredDimension(
                measuredWidth,
                if (measuredHeight > measuredWidth) measuredWidth else measuredHeight
            )
        }
    }
}
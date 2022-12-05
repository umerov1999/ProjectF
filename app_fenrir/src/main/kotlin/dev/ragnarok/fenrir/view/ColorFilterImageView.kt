package dev.ragnarok.fenrir.view

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import dev.ragnarok.fenrir.R

class ColorFilterImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {
    private var color = 0
    private var disabledColorFilter = false
    private fun init(context: Context, attrs: AttributeSet?) {
        val attrArray = context.obtainStyledAttributes(attrs, R.styleable.ColorFilterImageView)
        color = try {
            attrArray.getColor(
                R.styleable.ColorFilterImageView_filter_color,
                Color.BLACK
            )
        } finally {
            attrArray.recycle()
        }
        resolveColorFilter()
    }

    private fun resolveColorFilter() {
        imageTintList = if (disabledColorFilter) {
            null
        } else {
            ColorStateList.valueOf(color)
        }
    }

    fun setColorFilterEnabled(enabled: Boolean) {
        disabledColorFilter = !enabled
        resolveColorFilter()
    }

    init {
        init(context, attrs)
    }
}
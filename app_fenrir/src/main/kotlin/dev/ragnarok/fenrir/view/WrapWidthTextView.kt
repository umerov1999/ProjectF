package dev.ragnarok.fenrir.view

import android.content.Context
import android.text.Layout
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import dev.ragnarok.fenrir.R
import kotlin.math.ceil

open class WrapWidthTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {
    private var mFixWrapText = false
    private fun init(context: Context, attributeSet: AttributeSet?) {
        val a =
            context.theme.obtainStyledAttributes(attributeSet, R.styleable.WrapWidthTextView, 0, 0)
        mFixWrapText = try {
            a.getBoolean(R.styleable.WrapWidthTextView_fixWrapText, false)
        } finally {
            a.recycle()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mFixWrapText) {
            if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
                val width = getMaxWidth(layout)
                if (width in 1 until measuredWidth) {
                    super.onMeasure(
                        MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST),
                        heightMeasureSpec
                    )
                }
            }
        }
    }

    private fun getMaxWidth(layout: Layout): Int {
        val linesCount = layout.lineCount
        if (linesCount < 2) {
            return 0
        }
        var maxWidth = 0f
        for (i in 0 until linesCount) {
            maxWidth = maxWidth.coerceAtLeast(layout.getLineWidth(i))
        }
        return ceil(maxWidth.toDouble()).toInt()
    }

    init {
        init(context, attrs)
    }
}
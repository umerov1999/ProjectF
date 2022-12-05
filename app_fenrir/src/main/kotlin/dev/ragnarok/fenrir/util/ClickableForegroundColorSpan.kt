package dev.ragnarok.fenrir.util

import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt

class ClickableForegroundColorSpan(
    @ColorInt private val mColor: Int,
    private val mOnHashTagClickListener: OnHashTagClickListener?
) : ClickableSpan() {
    override fun updateDrawState(ds: TextPaint) {
        ds.color = mColor
    }

    override fun onClick(widget: View) {
        val text = (widget as TextView).text
        val s = text as Spanned
        val start = s.getSpanStart(this)
        val end = s.getSpanEnd(this)
        mOnHashTagClickListener?.onHashTagClicked(text.subSequence(start, end).toString())
    }

    interface OnHashTagClickListener {
        fun onHashTagClicked(hashTag: String)
    }

    init {
        if (mOnHashTagClickListener == null) {
            throw RuntimeException("constructor, click listener not specified. Are you sure you need to use this class?")
        }
    }
}
package dev.ragnarok.fenrir.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.settings.CurrentTheme
import dev.ragnarok.fenrir.util.Utils

class MySpinnerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    RelativeLayout(context, attrs) {
    private var mHintText: String? = null

    @ColorInt
    private var mHintColor = 0

    @ColorInt
    private var mTextColor = 0
    private var mTextView: TextView? = null
    private fun init(context: Context, attrs: AttributeSet?) {
        inflate(context, R.layout.view_my_spinner, this)
        mTextView = findViewById(R.id.text)
        setBackgroundResource(R.drawable.backgroud_rectangle_border)
        val icon = findViewById<ImageView>(R.id.icon)
        val a = context.obtainStyledAttributes(attrs, R.styleable.MySpinnerView)
        try {
            mHintText = a.getString(R.styleable.MySpinnerView_spinner_hint)
            mHintColor = a.getColor(
                R.styleable.MySpinnerView_spinner_hint_color,
                CurrentTheme.getColorSecondary(context)
            )
            mTextColor = a.getColor(
                R.styleable.MySpinnerView_spinner_text_color,
                CurrentTheme.getColorOnSurface(context)
            )
            val iconColor = a.getColor(
                R.styleable.MySpinnerView_spinner_icon_color,
                CurrentTheme.getColorPrimary(context)
            )
            Utils.setColorFilter(icon, iconColor)
        } finally {
            a.recycle()
        }
        mTextView?.text = mHintText
        mTextView?.setTextColor(mHintColor)
    }

    fun setIconOnClickListener(listener: OnClickListener?) {
        findViewById<View>(R.id.icon).setOnClickListener(listener)
    }

    fun setValue(value: String?) {
        if (value != null) {
            mTextView?.text = value
            mTextView?.setTextColor(mTextColor)
        } else {
            mTextView?.text = mHintText
            mTextView?.setTextColor(mHintColor)
        }
    }

    init {
        init(context, attrs)
    }
}
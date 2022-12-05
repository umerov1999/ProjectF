package dev.ragnarok.fenrir.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.util.Utils

class YoutubeButton @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    private fun init(context: Context, attrs: AttributeSet?) {
        inflate(context, R.layout.view_youtube_button, this)
        val a = context.obtainStyledAttributes(attrs, R.styleable.YoutubeButton)
        try {
            @DrawableRes val iconres = a.getResourceId(
                R.styleable.YoutubeButton_youtube_button_icon,
                R.drawable.heart_filled
            )
            val iconcolor =
                a.getColor(R.styleable.YoutubeButton_youtube_button_icon_color, Color.GRAY)
            val text = a.getString(R.styleable.YoutubeButton_youtube_button_text)
            val textcolor =
                a.getColor(R.styleable.YoutubeButton_youtube_button_text_color, Color.GRAY)
            val icon =
                findViewById<ImageView>(R.id.youtube_button_icon)
            Utils.setColorFilter(icon, iconcolor)
            icon.setImageResource(iconres)
            val textView = findViewById<TextView>(R.id.youtube_button_text)
            textView.setTextColor(textcolor)
            textView.text = text
        } finally {
            a.recycle()
        }
    }

    fun setIconColor(@ColorInt color: Int) {
        (findViewById<View>(R.id.youtube_button_icon) as ImageView).setColorFilter(color)
    }

    init {
        init(context, attrs)
    }
}
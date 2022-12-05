package dev.ragnarok.fenrir.view.emoji

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputEditText
import dev.ragnarok.fenrir.EmojiconHandler
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.settings.Settings

class EmojiconEditText @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    TextInputEditText(
        context, attrs
    ) {
    private var mEmojiconSize = 0
    private fun init(attrs: AttributeSet?) {
        mEmojiconSize = textSize.toInt()
        @SuppressLint("CustomViewStyleable") val a =
            context.obtainStyledAttributes(attrs, R.styleable.Emojicon)
        mEmojiconSize = a.getDimension(R.styleable.Emojicon_emojiconSize, textSize).toInt()
        a.recycle()
        text = text
    }

    override fun onTextChanged(
        text: CharSequence,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        if (!isInEditMode && !Settings.get().ui().isSystemEmoji) {
            getText()?.let { EmojiconHandler.addEmojis(context, it, mEmojiconSize) }
        }
    }

    /**
     * Set the size of emojicon in pixels.
     */
    fun setEmojiconSize(pixels: Int) {
        mEmojiconSize = pixels
    }

    init {
        init(attrs)
    }
}
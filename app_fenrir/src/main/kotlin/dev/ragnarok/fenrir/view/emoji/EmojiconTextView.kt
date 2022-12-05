package dev.ragnarok.fenrir.view.emoji

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.View
import androidx.fragment.app.FragmentActivity
import dev.ragnarok.fenrir.EmojiconHandler.addEmojis
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.link.LinkHelper
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.ModalBottomSheetDialogFragment
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.Option
import dev.ragnarok.fenrir.modalbottomsheetdialogfragment.OptionRequest
import dev.ragnarok.fenrir.settings.AppPrefs
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.util.ClickableForegroundColorSpan
import dev.ragnarok.fenrir.view.WrapWidthTextView
import java.util.regex.Pattern

class EmojiconTextView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    WrapWidthTextView(
        context, attrs
    ), ClickableForegroundColorSpan.OnHashTagClickListener {
    private var mEmojiconSize = 0
    private var mTextStart = 0
    private var mTextLength = -1
    private var mAdditionalHashTagChars: MutableList<Char>? = null
    private var mOnHashTagClickListener: OnHashTagClickListener? = null
    private var mDisplayHashTags = false
    private var mHashTagWordColor = 0
    private fun init(attrs: AttributeSet?) {
        mAdditionalHashTagChars = ArrayList(2)
        mAdditionalHashTagChars?.add('_')
        mAdditionalHashTagChars?.add('@')
        if (attrs == null) {
            mEmojiconSize = textSize.toInt()
        } else {
            @SuppressLint("CustomViewStyleable") val a =
                context.obtainStyledAttributes(attrs, R.styleable.Emojicon)
            try {
                mEmojiconSize = a.getDimension(R.styleable.Emojicon_emojiconSize, textSize).toInt()
                mTextStart = a.getInteger(R.styleable.Emojicon_emojiconTextStart, 0)
                mTextLength = a.getInteger(R.styleable.Emojicon_emojiconTextLength, -1)
                mHashTagWordColor = a.getColor(R.styleable.Emojicon_hashTagColor, Color.BLUE)
                mDisplayHashTags = a.getBoolean(R.styleable.Emojicon_displayHashTags, false)
            } finally {
                a.recycle()
            }
        }
        text = text
    }

    private fun setColorsToAllHashTags(text: Spannable) {
        var startIndexOfNextHashSign: Int
        var index = 0
        while (index < text.length - 1) {
            val sign = text[index]
            var nextNotLetterDigitCharIndex =
                index + 1 // we assume it is next. if if was not changed by findNextValidHashTagChar then index will be incremented by 1
            if (sign == '#') {
                startIndexOfNextHashSign = index
                nextNotLetterDigitCharIndex =
                    findNextValidHashTagChar(text, startIndexOfNextHashSign)
                setColorForHashTagToTheEnd(
                    text,
                    startIndexOfNextHashSign,
                    nextNotLetterDigitCharIndex
                )
            }
            index = nextNotLetterDigitCharIndex
        }
    }

    private fun findNextValidHashTagChar(text: CharSequence, start: Int): Int {
        var nonLetterDigitCharIndex = -1 // skip first sign '#"
        for (index in start + 1 until text.length) {
            val sign = text[index]
            val isValidSign =
                Character.isLetterOrDigit(sign) || mAdditionalHashTagChars?.contains(sign) == true
            if (!isValidSign) {
                nonLetterDigitCharIndex = index
                break
            }
        }
        if (nonLetterDigitCharIndex == -1) {
            // we didn't find non-letter. We are at the end of text
            nonLetterDigitCharIndex = text.length
        }
        return nonLetterDigitCharIndex
    }

    private fun setColorForHashTagToTheEnd(
        s: Spannable,
        startIndex: Int,
        nextNotLetterDigitCharIndex: Int
    ) {
        val span: CharacterStyle = if (mOnHashTagClickListener != null) {
            ClickableForegroundColorSpan(mHashTagWordColor, this)
        } else {
            // no need for clickable span because it is messing with selection when click
            ForegroundColorSpan(mHashTagWordColor)
        }
        s.setSpan(span, startIndex, nextNotLetterDigitCharIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    override fun setText(originalText: CharSequence?, type: BufferType) {
        if (originalText?.isNotEmpty() == true) {
            val spannable: Spannable = SpannableStringBuilder.valueOf(originalText)
            if (mDisplayHashTags) {
                setColorsToAllHashTags(spannable)
            }
            if (isInEditMode || !Settings.get().ui().isSystemEmoji) {
                addEmojis(context, spannable, mEmojiconSize, mTextStart, mTextLength)
            }
            val mode = if (isInEditMode) 0 else Settings.get().main().isOpenUrlInternal
            if (mode > 0) {
                linkifyVKUrl(spannable)
            }
            if (mode > 1) {
                linkifyNonVKUrl(spannable)
            }
            super.setText(spannable, type)
        } else {
            super.setText(originalText, type)
        }
    }

    private fun linkifyVKUrl(spannable: Spannable) {
        val m = URL_VK_PATTERN.matcher(spannable)
        while (m.find()) {
            val url = spannable.toString().substring(m.start(), m.end())
            val urlSpan: ClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    LinkHelper.openUrl(
                        context as Activity,
                        Settings.get().accounts().current,
                        url,
                        false
                    )
                }
            }
            spannable.setSpan(urlSpan, m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun linkifyNonVKUrl(spannable: Spannable) {
        val m = URL_NON_VK_PATTERN.matcher(spannable)
        while (m.find()) {
            val url = spannable.toString().substring(m.start(), m.end())
            val urlSpan: ClickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    if (URL_YOUTUBE_PATTERN.matcher(url).find()) {
                        val menus = ModalBottomSheetDialogFragment.Builder()
                        val hasVanced = AppPrefs.isVancedYoutubeInstalled(context)
                        if (hasVanced) {
                            menus.add(
                                OptionRequest(
                                    1,
                                    context.getString(R.string.title_play_in_youtube_vanced),
                                    R.drawable.ic_play_youtube,
                                    true
                                )
                            )
                        }
                        menus.add(
                            OptionRequest(
                                2,
                                context.getString(R.string.title_play_in_newpipe),
                                R.drawable.ic_new_pipe,
                                true
                            )
                        )
                        if (!hasVanced && AppPrefs.isYoutubeInstalled(context)) {
                            menus.add(
                                OptionRequest(
                                    3,
                                    context.getString(R.string.title_play_in_youtube),
                                    R.drawable.ic_play_youtube,
                                    true
                                )
                            )
                        }
                        menus.add(
                            OptionRequest(
                                4,
                                context.getString(R.string.title_play_in_another_software),
                                R.drawable.ic_external,
                                true
                            )
                        )
                        menus.header(
                            url,
                            R.drawable.ic_play_youtube,
                            null
                        )
                        menus.columns(1)
                        menus.show(
                            (context as FragmentActivity).supportFragmentManager,
                            "url_options",
                            object : ModalBottomSheetDialogFragment.Listener {
                                override fun onModalOptionSelected(option: Option) {
                                    when (option.id) {
                                        1 -> {
                                            val intent = Intent()
                                            intent.data = Uri.parse(url)
                                            intent.action = Intent.ACTION_VIEW
                                            intent.component = ComponentName(
                                                "app.revanced.android.youtube",
                                                "com.google.android.apps.youtube.app.application.Shell\$UrlActivity"
                                            )
                                            context.startActivity(intent)
                                        }
                                        2 -> {
                                            if (AppPrefs.isNewPipeInstalled(context)) {
                                                val intent = Intent()
                                                intent.data = Uri.parse(url)
                                                intent.action = Intent.ACTION_VIEW
                                                intent.component = ComponentName(
                                                    "org.schabi.newpipe",
                                                    "org.schabi.newpipe.RouterActivity"
                                                )
                                                context.startActivity(intent)
                                            } else {
                                                LinkHelper.openLinkInBrowser(
                                                    context,
                                                    "https://github.com/TeamNewPipe/NewPipe/releases"
                                                )
                                            }
                                        }
                                        3 -> {
                                            val intent = Intent()
                                            intent.data = Uri.parse(url)
                                            intent.action = Intent.ACTION_VIEW
                                            intent.component = ComponentName(
                                                "com.google.android.youtube",
                                                "com.google.android.apps.youtube.app.application.Shell\$UrlActivity"
                                            )
                                            context.startActivity(intent)
                                        }
                                        4 -> {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                            context.startActivity(intent)
                                        }
                                    }
                                }
                            })
                    } else {
                        LinkHelper.openLinkInBrowser(context, url)
                    }
                }
            }
            spannable.setSpan(urlSpan, m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /**
     * Set the size of emojicon in pixels.
     */
    fun setEmojiconSize(pixels: Int) {
        mEmojiconSize = pixels
    }

    private fun eraseAndColorizeAllText(text: Spannable) {
        if (getText() is Spannable) {
            val spannable = getText() as Spannable
            val spans = spannable.getSpans(0, text.length, CharacterStyle::class.java)
            for (span in spans) {
                spannable.removeSpan(span)
            }
        }
        setColorsToAllHashTags(text)
    }

    override fun onHashTagClicked(hashTag: String) {
        mOnHashTagClickListener?.onHashTagClicked(hashTag)
    }

    fun setOnHashTagClickListener(onHashTagClickListener: OnHashTagClickListener?) {
        mOnHashTagClickListener = onHashTagClickListener
    }

    fun setAdditionalHashTagChars(additionalHashTagChars: MutableList<Char>?) {
        mAdditionalHashTagChars = additionalHashTagChars
    }

    private fun getAllHashTags(withHashes: Boolean): List<String> {
        val text = text.toString()
        val spannable = getText() as Spannable

        // use set to exclude duplicates
        val hashTags: MutableSet<String> = LinkedHashSet()
        for (span in spannable.getSpans(0, text.length, CharacterStyle::class.java)) {
            hashTags.add(
                text.substring(
                    if (!withHashes) spannable.getSpanStart(span) + 1 else spannable.getSpanStart(
                        span
                    ), spannable.getSpanEnd(span)
                )
            )
        }
        return ArrayList(hashTags)
    }

    val allHashTags: List<String>
        get() = getAllHashTags(false)

    interface OnHashTagClickListener {
        fun onHashTagClicked(hashTag: String)
    }

    companion object {
        private val URL_VK_PATTERN =
            Pattern.compile("(((http|https|rstp)://)?(\\w+.)?vk\\.(com|me|cc)/\\S*)")
        private val URL_YOUTUBE_PATTERN =
            Pattern.compile("(((http|https|rstp)://)?(\\w+.)?(youtube\\.com|youtu\\.be)/\\S*)")
        private val URL_NON_VK_PATTERN =
            Pattern.compile("((http|https|rstp)://(?!(\\w+.)?vk\\.(com|me|cc)/)\\S*)")
    }

    init {
        init(attrs)
    }
}
package dev.ragnarok.fenrir

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.Spanned
import android.text.style.DynamicDrawableSpan
import android.util.SparseIntArray
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import dev.ragnarok.fenrir_common.BuildConfig
import dev.ragnarok.fenrir_common.R

object EmojiconHandler {
    internal class EmojiconSpan(
        private val mContext: Context,
        private val mResourceId: Int,
        private val mSize: Int
    ) : DynamicDrawableSpan() {
        private var mDrawable: Drawable? = null
        override fun getDrawable(): Drawable? {
            if (mDrawable == null) {
                try {
                    mDrawable = AppCompatResources.getDrawable(mContext, mResourceId)
                    mDrawable?.setBounds(0, 0, mSize, mSize)
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        e.printStackTrace()
                    }
                }
            }
            return mDrawable
        }
    }

    @DrawableRes
    private val sEmojisMap = SparseIntArray(162)

    @DrawableRes
    private fun getEmojiResource(codePoint: Int): Int {
        return sEmojisMap[codePoint]
    }

    fun addEmojis(
        context: Context,
        text: Spannable,
        emojiSize: Int,
        index: Int = 0,
        length: Int = -1
    ) {
        val textLength = text.length
        val textLengthToProcessMax = textLength - index
        val textLengthToProcess =
            if (length < 0 || length >= textLengthToProcessMax) textLength else length + index
        val oldSpans = text.getSpans(0, textLength, EmojiconSpan::class.java)
        for (oldSpan in oldSpans) {
            text.removeSpan(oldSpan)
        }
        var skip: Int
        var i = index
        while (i < textLengthToProcess) {
            var icon = 0
            val unicode = Character.codePointAt(text, i)
            skip = Character.charCount(unicode)
            if (unicode > 0xff) {
                icon = getEmojiResource(unicode)
            }
            if (icon > 0) {
                text.setSpan(
                    EmojiconSpan(context, icon, emojiSize),
                    i,
                    i + skip,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            i += skip
        }
    }

    init {
        sEmojisMap.put(0x1f31a, R.drawable.z_emoji_1f31a)
        sEmojisMap.put(0x1f434, R.drawable.z_emoji_1f434)
        sEmojisMap.put(0x1f44e, R.drawable.z_emoji_1f44e)
        sEmojisMap.put(0x1f49b, R.drawable.z_emoji_1f49b)
        sEmojisMap.put(0x1f605, R.drawable.z_emoji_1f605)
        sEmojisMap.put(0x1f617, R.drawable.z_emoji_1f617)
        sEmojisMap.put(0x1f629, R.drawable.z_emoji_1f629)
        sEmojisMap.put(0x1f63b, R.drawable.z_emoji_1f63b)
        sEmojisMap.put(0x1f913, R.drawable.z_emoji_1f913)
        sEmojisMap.put(0x1f31d, R.drawable.z_emoji_1f31d)
        sEmojisMap.put(0x1f435, R.drawable.z_emoji_1f435)
        sEmojisMap.put(0x1f44f, R.drawable.z_emoji_1f44f)
        sEmojisMap.put(0x1f49c, R.drawable.z_emoji_1f49c)
        sEmojisMap.put(0x1f606, R.drawable.z_emoji_1f606)
        sEmojisMap.put(0x1f618, R.drawable.z_emoji_1f618)
        sEmojisMap.put(0x1f62a, R.drawable.z_emoji_1f62a)
        sEmojisMap.put(0x1f63c, R.drawable.z_emoji_1f63c)
        sEmojisMap.put(0x1f914, R.drawable.z_emoji_1f914)
        sEmojisMap.put(0x1f31e, R.drawable.z_emoji_1f31e)
        sEmojisMap.put(0x1f436, R.drawable.z_emoji_1f436)
        sEmojisMap.put(0x1f450, R.drawable.z_emoji_1f450)
        sEmojisMap.put(0x1f49d, R.drawable.z_emoji_1f49d)
        sEmojisMap.put(0x1f607, R.drawable.z_emoji_1f607)
        sEmojisMap.put(0x1f619, R.drawable.z_emoji_1f619)
        sEmojisMap.put(0x1f62b, R.drawable.z_emoji_1f62b)
        sEmojisMap.put(0x1f63d, R.drawable.z_emoji_1f63d)
        sEmojisMap.put(0x1f915, R.drawable.z_emoji_1f915)
        sEmojisMap.put(0x1f412, R.drawable.z_emoji_1f412)
        sEmojisMap.put(0x1f437, R.drawable.z_emoji_1f437)
        sEmojisMap.put(0x1f479, R.drawable.z_emoji_1f479)
        sEmojisMap.put(0x1f49e, R.drawable.z_emoji_1f49e)
        sEmojisMap.put(0x1f608, R.drawable.z_emoji_1f608)
        sEmojisMap.put(0x1f61a, R.drawable.z_emoji_1f61a)
        sEmojisMap.put(0x1f62c, R.drawable.z_emoji_1f62c)
        sEmojisMap.put(0x1f63e, R.drawable.z_emoji_1f63e)
        sEmojisMap.put(0x1f916, R.drawable.z_emoji_1f916)
        sEmojisMap.put(0x1f414, R.drawable.z_emoji_1f414)
        sEmojisMap.put(0x1f438, R.drawable.z_emoji_1f438)
        sEmojisMap.put(0x1f47a, R.drawable.z_emoji_1f47a)
        sEmojisMap.put(0x1f49f, R.drawable.z_emoji_1f49f)
        sEmojisMap.put(0x1f609, R.drawable.z_emoji_1f609)
        sEmojisMap.put(0x1f61b, R.drawable.z_emoji_1f61b)
        sEmojisMap.put(0x1f62d, R.drawable.z_emoji_1f62d)
        sEmojisMap.put(0x1f63f, R.drawable.z_emoji_1f63f)
        sEmojisMap.put(0x1f917, R.drawable.z_emoji_1f917)
        sEmojisMap.put(0x1f417, R.drawable.z_emoji_1f417)
        sEmojisMap.put(0x1f439, R.drawable.z_emoji_1f439)
        sEmojisMap.put(0x1f47b, R.drawable.z_emoji_1f47b)
        sEmojisMap.put(0x1f4a4, R.drawable.z_emoji_1f4a4)
        sEmojisMap.put(0x1f60a, R.drawable.z_emoji_1f60a)
        sEmojisMap.put(0x1f61c, R.drawable.z_emoji_1f61c)
        sEmojisMap.put(0x1f62e, R.drawable.z_emoji_1f62e)
        sEmojisMap.put(0x1f640, R.drawable.z_emoji_1f640)
        sEmojisMap.put(0x1f918, R.drawable.z_emoji_1f918)
        sEmojisMap.put(0x1f419, R.drawable.z_emoji_1f419)
        sEmojisMap.put(0x1f43a, R.drawable.z_emoji_1f43a)
        sEmojisMap.put(0x1f47d, R.drawable.z_emoji_1f47d)
        sEmojisMap.put(0x1f4a5, R.drawable.z_emoji_1f4a5)
        sEmojisMap.put(0x1f60b, R.drawable.z_emoji_1f60b)
        sEmojisMap.put(0x1f61d, R.drawable.z_emoji_1f61d)
        sEmojisMap.put(0x1f62f, R.drawable.z_emoji_1f62f)
        sEmojisMap.put(0x1f641, R.drawable.z_emoji_1f641)
        sEmojisMap.put(0x1f981, R.drawable.z_emoji_1f981)
        sEmojisMap.put(0x1f423, R.drawable.z_emoji_1f423)
        sEmojisMap.put(0x1f43b, R.drawable.z_emoji_1f43b)
        sEmojisMap.put(0x1f47f, R.drawable.z_emoji_1f47f)
        sEmojisMap.put(0x1f4a9, R.drawable.z_emoji_1f4a9)
        sEmojisMap.put(0x1f60c, R.drawable.z_emoji_1f60c)
        sEmojisMap.put(0x1f61e, R.drawable.z_emoji_1f61e)
        sEmojisMap.put(0x1f630, R.drawable.z_emoji_1f630)
        sEmojisMap.put(0x1f642, R.drawable.z_emoji_1f642)
        sEmojisMap.put(0x1f984, R.drawable.z_emoji_1f984)
        sEmojisMap.put(0x1f424, R.drawable.z_emoji_1f424)
        sEmojisMap.put(0x1f43c, R.drawable.z_emoji_1f43c)
        sEmojisMap.put(0x1f480, R.drawable.z_emoji_1f480)
        sEmojisMap.put(0x1f4aa, R.drawable.z_emoji_1f4aa)
        sEmojisMap.put(0x1f60d, R.drawable.z_emoji_1f60d)
        sEmojisMap.put(0x1f61f, R.drawable.z_emoji_1f61f)
        sEmojisMap.put(0x1f631, R.drawable.z_emoji_1f631)
        sEmojisMap.put(0x1f643, R.drawable.z_emoji_1f643)
        sEmojisMap.put(0x261d, R.drawable.z_emoji_261d)
        sEmojisMap.put(0x1f425, R.drawable.z_emoji_1f425)
        sEmojisMap.put(0x1f43d, R.drawable.z_emoji_1f43d)
        sEmojisMap.put(0x1f48b, R.drawable.z_emoji_1f48b)
        sEmojisMap.put(0x1f525, R.drawable.z_emoji_1f525)
        sEmojisMap.put(0x1f60e, R.drawable.z_emoji_1f60e)
        sEmojisMap.put(0x1f620, R.drawable.z_emoji_1f620)
        sEmojisMap.put(0x1f632, R.drawable.z_emoji_1f632)
        sEmojisMap.put(0x1f644, R.drawable.z_emoji_1f644)
        sEmojisMap.put(0x2639, R.drawable.z_emoji_2639)
        sEmojisMap.put(0x1f426, R.drawable.z_emoji_1f426)
        sEmojisMap.put(0x1f446, R.drawable.z_emoji_1f446)
        sEmojisMap.put(0x1f493, R.drawable.z_emoji_1f493)
        sEmojisMap.put(0x1f590, R.drawable.z_emoji_1f590)
        sEmojisMap.put(0x1f60f, R.drawable.z_emoji_1f60f)
        sEmojisMap.put(0x1f621, R.drawable.z_emoji_1f621)
        sEmojisMap.put(0x1f633, R.drawable.z_emoji_1f633)
        sEmojisMap.put(0x1f648, R.drawable.z_emoji_1f648)
        sEmojisMap.put(0x263a, R.drawable.z_emoji_263a)
        sEmojisMap.put(0x1f427, R.drawable.z_emoji_1f427)
        sEmojisMap.put(0x1f447, R.drawable.z_emoji_1f447)
        sEmojisMap.put(0x1f494, R.drawable.z_emoji_1f494)
        sEmojisMap.put(0x1f595, R.drawable.z_emoji_1f595)
        sEmojisMap.put(0x1f610, R.drawable.z_emoji_1f610)
        sEmojisMap.put(0x1f622, R.drawable.z_emoji_1f622)
        sEmojisMap.put(0x1f634, R.drawable.z_emoji_1f634)
        sEmojisMap.put(0x1f649, R.drawable.z_emoji_1f649)
        sEmojisMap.put(0x26a1, R.drawable.z_emoji_26a1)
        sEmojisMap.put(0x1f428, R.drawable.z_emoji_1f428)
        sEmojisMap.put(0x1f448, R.drawable.z_emoji_1f448)
        sEmojisMap.put(0x1f495, R.drawable.z_emoji_1f495)
        sEmojisMap.put(0x1f596, R.drawable.z_emoji_1f596)
        sEmojisMap.put(0x1f611, R.drawable.z_emoji_1f611)
        sEmojisMap.put(0x1f623, R.drawable.z_emoji_1f623)
        sEmojisMap.put(0x1f635, R.drawable.z_emoji_1f635)
        sEmojisMap.put(0x1f64a, R.drawable.z_emoji_1f64a)
        sEmojisMap.put(0x270a, R.drawable.z_emoji_270a)
        sEmojisMap.put(0x1f42d, R.drawable.z_emoji_1f42d)
        sEmojisMap.put(0x1f449, R.drawable.z_emoji_1f449)
        sEmojisMap.put(0x1f496, R.drawable.z_emoji_1f496)
        sEmojisMap.put(0x1f600, R.drawable.z_emoji_1f600)
        sEmojisMap.put(0x1f612, R.drawable.z_emoji_1f612)
        sEmojisMap.put(0x1f624, R.drawable.z_emoji_1f624)
        sEmojisMap.put(0x1f636, R.drawable.z_emoji_1f636)
        sEmojisMap.put(0x1f64c, R.drawable.z_emoji_1f64c)
        sEmojisMap.put(0x270b, R.drawable.z_emoji_270b)
        sEmojisMap.put(0x1f42e, R.drawable.z_emoji_1f42e)
        sEmojisMap.put(0x1f44a, R.drawable.z_emoji_1f44a)
        sEmojisMap.put(0x1f497, R.drawable.z_emoji_1f497)
        sEmojisMap.put(0x1f601, R.drawable.z_emoji_1f601)
        sEmojisMap.put(0x1f613, R.drawable.z_emoji_1f613)
        sEmojisMap.put(0x1f625, R.drawable.z_emoji_1f625)
        sEmojisMap.put(0x1f637, R.drawable.z_emoji_1f637)
        sEmojisMap.put(0x1f64f, R.drawable.z_emoji_1f64f)
        sEmojisMap.put(0x270c, R.drawable.z_emoji_270c)
        sEmojisMap.put(0x1f42f, R.drawable.z_emoji_1f42f)
        sEmojisMap.put(0x1f44b, R.drawable.z_emoji_1f44b)
        sEmojisMap.put(0x1f498, R.drawable.z_emoji_1f498)
        sEmojisMap.put(0x1f602, R.drawable.z_emoji_1f602)
        sEmojisMap.put(0x1f614, R.drawable.z_emoji_1f614)
        sEmojisMap.put(0x1f626, R.drawable.z_emoji_1f626)
        sEmojisMap.put(0x1f638, R.drawable.z_emoji_1f638)
        sEmojisMap.put(0x1f910, R.drawable.z_emoji_1f910)
        sEmojisMap.put(0x2728, R.drawable.z_emoji_2728)
        sEmojisMap.put(0x1f430, R.drawable.z_emoji_1f430)
        sEmojisMap.put(0x1f44c, R.drawable.z_emoji_1f44c)
        sEmojisMap.put(0x1f499, R.drawable.z_emoji_1f499)
        sEmojisMap.put(0x1f603, R.drawable.z_emoji_1f603)
        sEmojisMap.put(0x1f615, R.drawable.z_emoji_1f615)
        sEmojisMap.put(0x1f627, R.drawable.z_emoji_1f627)
        sEmojisMap.put(0x1f639, R.drawable.z_emoji_1f639)
        sEmojisMap.put(0x1f911, R.drawable.z_emoji_1f911)
        sEmojisMap.put(0x2763, R.drawable.z_emoji_2763)
        sEmojisMap.put(0x1f431, R.drawable.z_emoji_1f431)
        sEmojisMap.put(0x1f44d, R.drawable.z_emoji_1f44d)
        sEmojisMap.put(0x1f49a, R.drawable.z_emoji_1f49a)
        sEmojisMap.put(0x1f604, R.drawable.z_emoji_1f604)
        sEmojisMap.put(0x1f616, R.drawable.z_emoji_1f616)
        sEmojisMap.put(0x1f628, R.drawable.z_emoji_1f628)
        sEmojisMap.put(0x1f63a, R.drawable.z_emoji_1f63a)
        sEmojisMap.put(0x1f912, R.drawable.z_emoji_1f912)
        sEmojisMap.put(0x2764, R.drawable.z_emoji_2764)
    }
}
package dev.ragnarok.fenrir.link

import android.content.Context
import android.text.SpannableStringBuilder
import java.util.regex.Pattern

object LinkParserFeedback {
    private val MENTIONS_PATTERN: Pattern =
        Pattern.compile("\\[((?:id|club|event|public)\\d+)\\|([^]]+)]")
    val MENTIONS_AVATAR_PATTERN: Pattern =
        Pattern.compile("\\[(id|club|event|public)(\\d+)\\|([^]]+)]")
    private val PHONE_NUMBER_PATTERN: Pattern = Pattern.compile("\\+\\d{8,15}")
    private var REPLY_URL_PATTERN: Pattern? = null
    private var URL_PATTERN: Pattern? = null
    private fun isNumber(str: String?): Boolean {
        return str != null && str.matches(Regex("\\d+"))
    }

    fun parseLinks(context: Context, text: CharSequence): SpannableStringBuilder {
        var spannableStringBuilder = SpannableStringBuilder(text)
        val matcher = REPLY_URL_PATTERN?.matcher(spannableStringBuilder)
        matcher?.let {
            var lastMatcherIndex = 0
            while (it.find()) {
                val jj = it.group(1)
                jj ?: continue
                val linkSpan = LinkSpan(context, jj, true)
                spannableStringBuilder.replace(
                    it.start() - lastMatcherIndex,
                    it.end() - lastMatcherIndex,
                    it.group(3)
                )
                spannableStringBuilder.setSpan(
                    linkSpan,
                    it.start() - lastMatcherIndex,
                    it.start() - lastMatcherIndex + (it.group(3)?.length ?: 0),
                    0
                )
                lastMatcherIndex += it.group().length - (it.group(3)?.length ?: 0)
            }
        }
        val matcher2 = URL_PATTERN?.matcher(spannableStringBuilder)
        matcher2?.let {
            while (it.find()) {
                spannableStringBuilder.setSpan(
                    LinkSpan(context, it.group(), true),
                    it.start(),
                    it.end(),
                    0
                )
            }
        }
        val matcher3 = PHONE_NUMBER_PATTERN.matcher(spannableStringBuilder)
        while (matcher3.find()) {
            spannableStringBuilder.setSpan(
                LinkSpan(context, "tel:" + matcher3.group(), false),
                matcher3.start(),
                matcher3.end(),
                0
            )
        }
        var lastMatcherIndex = 0
        val matcher5 = MENTIONS_PATTERN.matcher(spannableStringBuilder)
        while (matcher5.find()) {
            val linkSpan2 = LinkSpan(context, "https://vk.com/" + matcher5.group(1), false)
            val replace2 = spannableStringBuilder.replace(
                matcher5.start() - lastMatcherIndex,
                matcher5.end() - lastMatcherIndex,
                matcher5.group(2)
            )
            replace2.setSpan(
                linkSpan2,
                matcher5.start() - lastMatcherIndex,
                matcher5.start() - lastMatcherIndex + (matcher5.group(2)?.length ?: 0),
                0
            )
            lastMatcherIndex += matcher5.group().length - (matcher5.group(2)?.length ?: 0)
            spannableStringBuilder = replace2
        }
        return spannableStringBuilder
    }

    init {
        URL_PATTERN = null
        REPLY_URL_PATTERN = null
        try {
            URL_PATTERN =
                Pattern.compile("(?:http|https|Http|Https)?://(?:www\\.)?[-a-zA-Z\\x{0430}-\\x{044F}\\x{0410}-\\x{042F}0-9@:%._+~#=]{1,256}\\.[a-zA-Z\\x{0430}-\\x{044F}\\x{0410}-\\x{042F}0-9()]{1,6}\\S([-a-zA-Z\\x{0430}-\\x{044F}\\x{0410}-\\x{042F}0-9()@:%_+.~#?&/=]*)")
            REPLY_URL_PATTERN = Pattern.compile("\\[($URL_PATTERN)\\|([^]]+)]")
        } catch (_: Exception) {
        }
    }
}
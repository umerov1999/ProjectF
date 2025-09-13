package dev.ragnarok.fenrir.link

import android.content.Context
import android.text.SpannableStringBuilder
import dev.ragnarok.fenrir.orZero

object LinkParserFeedback {
    private val MENTIONS_PATTERN: Regex =
        Regex("\\[((?:id|club|event|public)\\d+)\\|([^]]+)]")
    val MENTIONS_AVATAR_PATTERN: Regex =
        Regex("\\[(id|club|event|public)(\\d+)\\|([^]]+)]")
    private val PHONE_NUMBER_PATTERN: Regex = Regex("\\+\\d{8,15}")
    private const val URL_REGEX_PATTERN =
        "(?:http|https|Http|Https)?://(?:www\\.)?[-a-zA-Z\\x{0430}-\\x{044F}\\x{0410}-\\x{042F}0-9@:%._+~#=]{1,256}\\.[a-zA-Z\\x{0430}-\\x{044F}\\x{0410}-\\x{042F}0-9()]{1,6}\\S([-a-zA-Z\\x{0430}-\\x{044F}\\x{0410}-\\x{042F}0-9()@:%_+.~#?&/=]*)"
    private var REPLY_URL_PATTERN: Regex = Regex("\\[($URL_REGEX_PATTERN)\\|([^]]+)]")
    private var URL_PATTERN: Regex = Regex(URL_REGEX_PATTERN)

    fun parseLinks(context: Context, text: CharSequence): SpannableStringBuilder {
        var spannableStringBuilder = SpannableStringBuilder(text)

        try {
            var res: MatchResult?
            do {
                res = REPLY_URL_PATTERN.find(spannableStringBuilder)
                if (res == null) {
                    continue
                }
                val jj = res.groupValues.getOrNull(1) ?: continue

                val linkSpan = LinkSpan(context, jj, true)
                spannableStringBuilder.replace(
                    res.range.start,
                    (res.range.last + 1),
                    res.groupValues.getOrNull(3)
                )
                spannableStringBuilder.setSpan(
                    linkSpan,
                    res.range.start,
                    res.range.start + (res.groupValues.getOrNull(3)?.length.orZero()),
                    0
                )
            } while (res != null)
        } catch (_: Exception) {
        }
        try {
            val res = URL_PATTERN.findAll(spannableStringBuilder)
            for (i in res) {
                spannableStringBuilder.setSpan(
                    LinkSpan(context, i.groupValues.getOrNull(0).orEmpty(), true),
                    i.range.start,
                    (i.range.last + 1),
                    0
                )
            }
        } catch (_: Exception) {
        }

        try {
            val res = PHONE_NUMBER_PATTERN.findAll(spannableStringBuilder)
            for (i in res) {
                spannableStringBuilder.setSpan(
                    LinkSpan(context, "tel:" + i.groupValues.getOrNull(0).orEmpty(), false),
                    i.range.start,
                    (i.range.last + 1),
                    0
                )
            }
        } catch (_: Exception) {
        }

        try {
            var res: MatchResult?
            do {
                res = MENTIONS_PATTERN.find(spannableStringBuilder)
                if (res == null) {
                    continue
                }
                val linkSpan2 = LinkSpan(
                    context,
                    "https://vk.ru/" + res.groupValues.getOrNull(1).orEmpty(),
                    false
                )
                val replace2 = spannableStringBuilder.replace(
                    res.range.start,
                    (res.range.last + 1),
                    res.groupValues.getOrNull(2).orEmpty()
                )
                replace2.setSpan(
                    linkSpan2,
                    res.range.start,
                    res.range.start + (res.groupValues.getOrNull(2)?.length.orZero()),
                    0
                )
                spannableStringBuilder = replace2
            } while (res != null)
        } catch (_: Exception) {
        }
        return spannableStringBuilder
    }
}
package dev.ragnarok.fenrir.link

import android.content.Context
import android.text.SpannableStringBuilder
import android.util.Patterns
import java.util.*
import java.util.regex.Pattern

object LinkParser {
    private val MENTIONS_PATTERN: Pattern =
        Pattern.compile("\\[((?:id|club|event|public)\\d+)\\|([^]]+)]")
    val MENTIONS_AVATAR_PATTERN: Pattern =
        Pattern.compile("\\[((?:id|club|event|public))(\\d+)\\|([^]]+)]")
    private val PHONE_NUMBER_PATTERN: Pattern = Pattern.compile("\\+\\d{8,15}")
    private var REPLY_URL_PATTERN: Pattern? = null
    private var URL_PATTERN: Pattern? = null
    private fun isNumber(str: String?): Boolean {
        return str != null && str.matches(Regex("\\d+"))
    }

    fun parseLinks(context: Context, charSequence: CharSequence): SpannableStringBuilder {
        REPLY_URL_PATTERN ?: return SpannableStringBuilder()
        val spannableStringBuilder: SpannableStringBuilder
        val r12: CharSequence
        val spannableStringBuilder2 = SpannableStringBuilder(charSequence)
        val matcher = REPLY_URL_PATTERN?.matcher(charSequence)
        var spannableStringBuilder3 = spannableStringBuilder2
        var i2 = 0
        while (matcher?.find() == true) {
            val jj = matcher.group(1)
            jj ?: continue
            val linkSpan = LinkSpan(context, jj, true)
            val replace = spannableStringBuilder3.replace(
                matcher.start() - i2,
                matcher.end() - i2,
                matcher.group(14)
            )
            replace.setSpan(
                linkSpan,
                matcher.start() - i2,
                matcher.start() - i2 + (matcher.group(14)?.length ?: 0),
                0
            )
            i2 += matcher.group().length - Objects.requireNonNull(matcher.group(14)).length
            spannableStringBuilder3 = replace
        }
        r12 = spannableStringBuilder3
        spannableStringBuilder = spannableStringBuilder3
        val matcher2 = URL_PATTERN?.matcher(r12) ?: return spannableStringBuilder
        while (matcher2.find()) {
            if (!isNumber(matcher2.group(6)) && (matcher2.start() <= 0 || spannableStringBuilder[matcher2.start() - 1] != '@')) {
                spannableStringBuilder.setSpan(
                    LinkSpan(context, matcher2.group(), true),
                    matcher2.start(),
                    matcher2.end(),
                    0
                )
            }
        }
        val matcher3 = PHONE_NUMBER_PATTERN.matcher(r12)
        while (matcher3.find()) {
            spannableStringBuilder.setSpan(
                LinkSpan(context, "tel:" + matcher3.group(), false),
                matcher3.start(),
                matcher3.end(),
                0
            )
        }
        val matcher5 = MENTIONS_PATTERN.matcher(r12)
        var spannableStringBuilder4 = spannableStringBuilder
        var i3 = 0
        while (matcher5.find()) {
            val linkSpan2 = LinkSpan(context, "https://vk.com/" + matcher5.group(1), false)
            val replace2 = spannableStringBuilder4.replace(
                matcher5.start() - i3,
                matcher5.end() - i3,
                matcher5.group(2)
            )
            replace2.setSpan(
                linkSpan2,
                matcher5.start() - i3,
                matcher5.start() - i3 + (matcher5.group(2)?.length ?: 0),
                0
            )
            i3 += matcher5.group().length - (matcher5.group(2)?.length ?: 0)
            spannableStringBuilder4 = replace2
        }
        return spannableStringBuilder4
    }

    init {
        URL_PATTERN = null
        REPLY_URL_PATTERN = null
        try {
            @Suppress("DEPRECATION")
            URL_PATTERN = Pattern.compile(
                "((?:(http|https|Http|Https|ftp|Ftp)://(?:(?:[a-zA-Z\\d$\\-_.+!*'(),;?&=]|(?:%[a-fA-F\\d]{2})){1,64}(?::(?:[a-zA-Z\\d$\\-_.+!*'(),;?&=]|(?:%[a-fA-F\\d]{2})){1,25})?@)?)?(?:" + Pattern.compile(
                    "(([a-zA-Z\\d -퟿豈-﷏ﷰ-￯]([a-zA-Z\\d -퟿豈-﷏ﷰ-￯\\-]{0,61}[a-zA-Z\\d -퟿豈-﷏ﷰ-￯])?\\.)+([a-zA-Z\\d-]{2,63}|рф|бел|укр)|" + Patterns.IP_ADDRESS + ")"
                ) + ")" + "(?::\\d{1,5})?)" + "(/(?:(?:[" + "a-zA-Z\\d -퟿豈-﷏ﷰ-￯" + ";/?:@&=#~" + "\\-.+!*'(),_])|(?:%[a-fA-F\\d]{2}))*)?" + "(?:\\b|$)"
            )
            REPLY_URL_PATTERN = Pattern.compile("\\[($URL_PATTERN)\\|([^]]+)]")
        } catch (ignored: Exception) {
        }
    }
}
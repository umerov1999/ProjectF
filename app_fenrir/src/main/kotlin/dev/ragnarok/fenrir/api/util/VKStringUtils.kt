package dev.ragnarok.fenrir.api.util

import java.util.regex.Pattern

/**
 * Helper class for join collections to strings
 */
object VKStringUtils {
    private const val pattern_string_profile_id = "^(id)?(\\d{1,10})$"
    private val pattern_profile_id = Pattern.compile(pattern_string_profile_id)

    fun join(delimiter: CharSequence?, tokens: Array<Any?>): String {
        val sb = StringBuilder()
        var firstTime = true
        for (token in tokens) {
            if (firstTime) {
                firstTime = false
            } else {
                sb.append(delimiter)
            }
            sb.append(token)
        }
        return sb.toString()
    }


    fun firstNonEmptyString(vararg array: String?): String? {
        for (s in array) {
            if (!s.isNullOrEmpty()) {
                return s
            }
        }
        return null
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     *
     * @param tokens an array objects to be joined. Strings will be formed from
     * the objects by calling object.toString().
     */

    fun join(delimiter: CharSequence?, tokens: Iterable<*>): String {
        val sb = StringBuilder()
        var firstTime = true
        for (token in tokens) {
            if (firstTime) {
                firstTime = false
            } else {
                sb.append(delimiter)
            }
            sb.append(token)
        }
        return sb.toString()
    }


    fun extractPattern(string: String?, pattern: String?): String? {
        string ?: return null
        pattern ?: return null
        val p = Pattern.compile(pattern)
        val m = p.matcher(string)
        return if (!m.find()) {
            null
        } else m.toMatchResult().group(1)
    }


    fun unescape(text: String?): String? {
        return text?.replace("&amp;", "&")?.replace("&quot;", "\"")?.replace("<br>", "\n")
            ?.replace("&gt;", ">")?.replace("&lt;", "<")?.replace("<br/>", "\n")
            ?.replace("&ndash;", "-")?.trim { it <= ' ' }

        //Баг в API
        //amp встречается в сообщении, br в Ответах тип comment_photo, gt lt на стене - баг API, ndash в статусе когда аудио транслируется
        //quot в тексте сообщения из LongPoll - то есть в уведомлении
    }


    fun parseProfileId(text: String?): String? {
        text ?: return null
        val m = pattern_profile_id.matcher(text)
        return if (!m.find()) {
            null
        } else m.group(2)
    }
}
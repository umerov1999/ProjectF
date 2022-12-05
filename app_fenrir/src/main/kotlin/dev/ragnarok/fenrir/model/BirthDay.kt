package dev.ragnarok.fenrir.model

import java.util.regex.Pattern

class BirthDay(val user: User) {
    val day: Int
    val month: Int
    val sortVt: Int

    init {
        var matcher = PATTERN_DAY_MONTH_YEAR.matcher(user.bdate.orEmpty())
        if (matcher.find()) {
            day = matcher.group(1)?.toInt() ?: 0
            month = matcher.group(2)?.toInt() ?: 0
            sortVt = day + (month * 30)
        } else {
            matcher = PATTERN_DAY_MONTH.matcher(user.bdate.orEmpty())
            if (matcher.find()) {
                day = matcher.group(1)?.toInt() ?: 0
                month = matcher.group(2)?.toInt() ?: 0
                sortVt = day + (month * 30)
            } else {
                day = 0
                month = 0
                sortVt = 0
            }
        }
    }

    companion object {
        val PATTERN_DAY_MONTH: Pattern = Pattern.compile("(\\d*)\\.(\\d*)")
        val PATTERN_DAY_MONTH_YEAR: Pattern = Pattern.compile("(\\d*)\\.(\\d*)\\.(\\d*)")
    }
}
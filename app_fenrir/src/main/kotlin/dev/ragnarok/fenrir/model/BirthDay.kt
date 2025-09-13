package dev.ragnarok.fenrir.model

import dev.ragnarok.fenrir.orZero

class BirthDay(val user: User) {
    val day: Int
    val month: Int
    val sortVt: Int

    init {
        var matcher = PATTERN_DAY_MONTH_YEAR.find(user.bdate.orEmpty())
        if (matcher != null) {
            day = matcher.groupValues.getOrNull(1)?.toInt().orZero()
            month = matcher.groupValues.getOrNull(2)?.toInt().orZero()
            sortVt = day + (month * 30)
        } else {
            matcher = PATTERN_DAY_MONTH.find(user.bdate.orEmpty())
            if (matcher != null) {
                day = matcher.groupValues.getOrNull(1)?.toInt().orZero()
                month = matcher.groupValues.getOrNull(2)?.toInt().orZero()
                sortVt = day + (month * 30)
            } else {
                day = 0
                month = 0
                sortVt = 0
            }
        }
    }

    companion object {
        val PATTERN_DAY_MONTH: Regex = Regex("(\\d*)\\.(\\d*)")
        val PATTERN_DAY_MONTH_YEAR: Regex = Regex("(\\d*)\\.(\\d*)\\.(\\d*)")
    }
}
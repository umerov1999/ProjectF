package dev.ragnarok.fenrir.util

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.R
import java.util.Calendar

object Month {

    @StringRes
    fun getMonthTitle(num: Int): Int {
        return when (num) {
            Calendar.JANUARY -> R.string.january
            Calendar.FEBRUARY -> R.string.february
            Calendar.MARCH -> R.string.march
            Calendar.APRIL -> R.string.april
            Calendar.MAY -> R.string.may
            Calendar.JUNE -> R.string.june
            Calendar.JULY -> R.string.july
            Calendar.AUGUST -> R.string.august
            Calendar.SEPTEMBER -> R.string.september
            Calendar.OCTOBER -> R.string.october
            Calendar.NOVEMBER -> R.string.november
            Calendar.DECEMBER -> R.string.december
            else -> throw IllegalArgumentException()
        }
    }
}
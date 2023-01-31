package dev.ragnarok.fenrir.util

import androidx.annotation.StringRes
import dev.ragnarok.fenrir.R
import java.util.Calendar

object Month {

    @StringRes
    fun getMonthTitle(num: Int): Int {
        when (num) {
            Calendar.JANUARY -> return R.string.january
            Calendar.FEBRUARY -> return R.string.february
            Calendar.MARCH -> return R.string.march
            Calendar.APRIL -> return R.string.april
            Calendar.MAY -> return R.string.may
            Calendar.JUNE -> return R.string.june
            Calendar.JULY -> return R.string.july
            Calendar.AUGUST -> return R.string.august
            Calendar.SEPTEMBER -> return R.string.september
            Calendar.OCTOBER -> return R.string.october
            Calendar.NOVEMBER -> return R.string.november
            Calendar.DECEMBER -> return R.string.december
        }
        throw IllegalArgumentException()
    }
}
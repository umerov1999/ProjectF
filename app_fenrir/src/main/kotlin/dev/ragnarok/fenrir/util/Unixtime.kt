package dev.ragnarok.fenrir.util

import java.util.*

object Unixtime {

    fun now(): Long {
        return System.currentTimeMillis() / 1000
    }

    fun of(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance()
        calendar[Calendar.YEAR] = year
        calendar[Calendar.MONTH] = month
        calendar[Calendar.DAY_OF_MONTH] = day
        calendar[Calendar.HOUR_OF_DAY] = hour
        calendar[Calendar.MINUTE] = minute
        return calendar.timeInMillis / 1000
    }
}
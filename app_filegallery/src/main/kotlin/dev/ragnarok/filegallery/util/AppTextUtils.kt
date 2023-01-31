package dev.ragnarok.filegallery.util

import android.content.Context
import dev.ragnarok.filegallery.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppTextUtils {
    private const val ONE_DAY_SEC = (24 * 60 * 60).toLong()
    private val DATE = Date()

    private var SHORT_DATE = SimpleDateFormat("HH:mm", Utils.appLocale)
    private var FULL_LITTLE_DATE = SimpleDateFormat("dd.MM.yy HH:mm", Utils.appLocale)


    fun updateDateLang(locale: Locale?) {
        SHORT_DATE = SimpleDateFormat("HH:mm", locale)
        FULL_LITTLE_DATE = SimpleDateFormat("dd.MM.yy HH:mm", locale)
    }

    /**
     * Получение строки с датой и временем сообщений
     *
     * @param unixTime дата в формате unix-time
     * @return строка с датой и временем
     */
    fun getDateFromUnixTimeShorted(context: Context, unixTime: Long): String {
        val calendar = Calendar.getInstance()
        calendar[calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DATE], 0, 0] =
            0
        DATE.time = unixTime * 1000
        val startToday = calendar.timeInMillis / 1000
        val startTomorrow = startToday + ONE_DAY_SEC
        val startOfDayAfterTomorrow = startTomorrow + ONE_DAY_SEC
        val startOfYesterday = startToday - ONE_DAY_SEC
        if (unixTime in startToday until startTomorrow) {
            return SHORT_DATE.format(DATE)
        }
        if (unixTime in startOfYesterday until startToday) {
            return context.getString(
                R.string.formatted_date_yesterday_clean,
                SHORT_DATE.format(DATE)
            )
        }
        return if (unixTime in startTomorrow until startOfDayAfterTomorrow) {
            context.getString(
                R.string.formatted_date_tomorrow_clean,
                SHORT_DATE.format(DATE)
            )
        } else FULL_LITTLE_DATE.format(DATE)
    }
}
package dev.ragnarok.fenrir.util

import android.content.Context
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.link.internal.OwnerLinkSpanFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object AppTextUtils {
    private const val ONE_DAY_SEC = (24 * 60 * 60).toLong()
    private val DATE = Date()
    private val CALENDAR = Calendar.getInstance()
    private const val K = "K"
    private const val ZERO = "0"
    private const val TWO_ZERO = "00"
    private const val POINT = "."
    private const val EMPTY = ""
    private var SHORT_DATE = SimpleDateFormat("HH:mm", Utils.appLocale)
    private var FULL_DATE = SimpleDateFormat("dd MMM yyyy HH:mm", Utils.appLocale)
    private var FULL_LITTLE_DATE = SimpleDateFormat("dd.MM.yy HH:mm", Utils.appLocale)


    fun updateDateLang(locale: Locale?) {
        SHORT_DATE = SimpleDateFormat("HH:mm", locale)
        FULL_DATE = SimpleDateFormat("dd MMM yyyy HH:mm", locale)
        FULL_LITTLE_DATE = SimpleDateFormat("dd.MM.yy HH:mm", locale)
    }


    fun safeTrim(text: String?, ifNull: String?): String? {
        return text?.trim { it <= ' ' } ?: ifNull
    }


    fun reduceStringForPost(input: String?): String? {
        val pp = OwnerLinkSpanFactory.findPatterns(input, owners = true, topics = false)
        var l = 400
        for (i in pp.orEmpty()) {
            if (i.start >= l) {
                break
            } else if (i.end > l) {
                l = i.end + 1
                break
            }

        }
        return reduceText(input, l)
    }


    fun reduceText(text: String?, maxLength: Int): String? {
        return if (text == null || text.length < maxLength) {
            text
        } else {
            text.substring(0, maxLength) + "..."
        }
    }

    /**
     * Получения строки с размером в мегабайтах
     *
     * @param size размер в байтах
     * @return строка типа "13.6 Mb"
     */

    fun getSizeString(size: Long): String {
        val sizeDouble = size.toDouble() / 1024 / 1024
        val newDouble = BigDecimal(sizeDouble).setScale(2, RoundingMode.UP).toDouble()
        return "$newDouble Mb"
    }

    /**
     * Получение строки с датой и временем сообщений
     *
     * @param unixTime дата в формате unix-time
     * @return строка с датой и временем
     */

    fun getDateFromUnixTime(unixTime: Long): String {
        CALENDAR.timeInMillis = System.currentTimeMillis()
        DATE.time = unixTime * 1000
        CALENDAR[CALENDAR[Calendar.YEAR], CALENDAR[Calendar.MONTH], CALENDAR[Calendar.DATE], 0, 0] =
            0
        return if (unixTime * 1000 > CALENDAR.timeInMillis) SHORT_DATE.format(DATE) else FULL_DATE.format(
            DATE
        )
    }

    /**
     * Получение строки с датой и временем сообщений
     *
     * @param unixTime дата в формате unix-time
     * @return строка с датой и временем
     */

    fun getDateFromUnixTime(context: Context, unixTime: Long): String {
        val calendar = Calendar.getInstance()
        calendar[calendar[Calendar.YEAR], calendar[Calendar.MONTH], calendar[Calendar.DATE], 0, 0] =
            0
        DATE.time = unixTime * 1000
        val startToday = calendar.timeInMillis / 1000
        val startTomorrow = startToday + ONE_DAY_SEC
        val startOfDayAfterTomorrow = startTomorrow + ONE_DAY_SEC
        val startOfYesterday = startToday - ONE_DAY_SEC
        if (unixTime in startToday until startTomorrow) {
            return context.getString(R.string.formatted_date_today, SHORT_DATE.format(DATE))
        }
        if (unixTime in startOfYesterday until startToday) {
            return context.getString(R.string.formatted_date_yesterday, SHORT_DATE.format(DATE))
        }
        return if (unixTime in startTomorrow until startOfDayAfterTomorrow) {
            context.getString(
                R.string.formatted_date_tomorrow,
                SHORT_DATE.format(DATE)
            )
        } else FULL_DATE.format(DATE)
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

    fun getDurationString(seconds: Int): String {
        var pSeconds = seconds
        val hours = pSeconds / 3600
        val minutes = pSeconds % 3600 / 60
        pSeconds %= 60
        return if (hours == 0) {
            twoDigitString(minutes) + ":" + twoDigitString(pSeconds)
        } else {
            twoDigitString(hours) + ":" + twoDigitString(minutes) + ":" + twoDigitString(
                pSeconds
            )
        }
    }


    fun getDurationStringMS(ms: Int): String {
        return getDurationString(ms / 1000)
    }

    private fun twoDigitString(number: Int): String {
        if (number == 0) {
            return TWO_ZERO
        }
        return if (number / 10 == 0) {
            ZERO + number
        } else number.toString()
    }


    fun getCounterWithK(counter: Int): String {
        val num = counter / 1000
        if (num in 1..9) {
            //return num + "." + (counter / 100) % 10 + K;
            return counter.toString()
        }
        return if (num >= 10) {
            num.toString() + K
        } else counter.toString()
    }


    fun getDateWithZeros(date: String?): String {
        if (date.isNullOrEmpty() || date == ZERO) {
            return EMPTY
        }
        val tmp = date.split(Pattern.quote(POINT)).toTypedArray()
        for (i in tmp.indices) {
            if (tmp[i].length == 1) {
                tmp[i] = ZERO + tmp[i]
            }
        }
        return Utils.join(POINT, tmp)
    }
}
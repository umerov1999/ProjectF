package dev.ragnarok.fenrir.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.DatePicker
import android.widget.TimePicker
import dev.ragnarok.fenrir.util.Logger
import dev.ragnarok.fenrir.util.Unixtime
import java.util.Calendar
import java.util.Date

class DateTimePicker internal constructor(builder: Builder) {
    private val time: Long = builder.pTime
    private val context: Context = builder.context
    private val callback: Callback? = builder.pCallback
    internal fun show() {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = time
        val year = calendar[Calendar.YEAR]
        val month = calendar[Calendar.MONTH]
        val day = calendar[Calendar.DAY_OF_MONTH]
        val hours = calendar[Calendar.HOUR_OF_DAY]
        val minutes = calendar[Calendar.MINUTE]
        Logger.d(TAG, "onTimerClick, init time: " + Date(time))
        DatePickerDialog(
            context,
            { _: DatePicker?, newYear: Int, newMonth: Int, newDay: Int ->
                showTime(
                    newYear,
                    newMonth,
                    newDay,
                    hours,
                    minutes
                )
            },
            year,
            month,
            day
        ).show()
    }

    private fun showTime(year: Int, month: Int, day: Int, hour: Int, minutes: Int) {
        TimePickerDialog(
            context,
            { _: TimePicker?, newHourOfDay: Int, newMinutes: Int ->
                callback?.onDateTimeSelected(
                    Unixtime.of(
                        year,
                        month,
                        day,
                        newHourOfDay,
                        newMinutes
                    )
                )
            },
            hour,
            minutes,
            true
        ).show()
    }

    interface Callback {
        fun onDateTimeSelected(unixtime: Long)
    }

    class Builder(val context: Context) {
        var pCallback: Callback? = null
        var pTime: Long
        fun setTime(unixtime: Long): Builder {
            pTime = unixtime * 1000
            return this
        }

        fun setCallback(callback: Callback?): Builder {
            this.pCallback = callback
            return this
        }

        fun show() {
            DateTimePicker(this).show()
        }

        init {
            pTime = System.currentTimeMillis()
        }
    }

    companion object {
        private val TAG = DateTimePicker::class.java.simpleName
    }

}
package dev.ragnarok.fenrir.model

class Day(var day: Int, var month: Int, var year: Int) {
    fun setDay(day: Int): Day {
        this.day = day
        return this
    }

    fun setMonth(month: Int): Day {
        this.month = month
        return this
    }

    fun setYear(year: Int): Day {
        this.year = year
        return this
    }
}
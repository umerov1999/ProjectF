package dev.ragnarok.fenrir.activity.captcha.sensors.model

@JvmInline
internal value class PeriodMs(val value: Int) {

    companion object {

        fun PeriodMs.toUs(): PeriodUs {
            return PeriodUs(this.value * 1000)
        }
    }
}
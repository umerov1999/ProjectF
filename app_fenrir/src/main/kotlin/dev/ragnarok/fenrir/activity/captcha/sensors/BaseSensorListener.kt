package dev.ragnarok.fenrir.activity.captcha.sensors

import dev.ragnarok.fenrir.activity.captcha.sensors.model.PeriodMs
import dev.ragnarok.fenrir.activity.captcha.sensors.model.SensorData

internal interface BaseSensorListener<T : SensorData> {

    // 200ms equals to value SensorManager.SENSOR_DELAY_NORMAL
    fun startListening(periodMs: PeriodMs = PeriodMs(200))

    fun stopListening()

    fun setOnSensorChangedListener(listener: (T) -> Unit)
}

package dev.ragnarok.fenrir.activity.captcha.sensors

import dev.ragnarok.fenrir.activity.captcha.sensors.model.PeriodMs
import dev.ragnarok.fenrir.activity.captcha.sensors.model.Sensors
import dev.ragnarok.fenrir.activity.captcha.sensors.model.SensorsData

internal interface SensorsDataRepository {

    fun startListening(
        sensors: List<Sensors>,
        periodMs: PeriodMs,
        onDataUpdate: (SensorsData) -> Unit
    )

    fun stopListening()
}

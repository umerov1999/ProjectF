package dev.ragnarok.fenrir.activity.captcha.sensors.model

import org.json.JSONObject

internal typealias SensorsData = List<SensorData>

internal fun SensorsData.toJson(): JSONObject {
    return JSONObject().apply {
        this@toJson.forEach {
            put(it.sensorName, it.toJson())
        }
    }
}
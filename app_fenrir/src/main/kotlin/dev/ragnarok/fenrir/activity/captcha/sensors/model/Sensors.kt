package dev.ragnarok.fenrir.activity.captcha.sensors.model

import android.util.Log
import org.json.JSONArray

internal enum class Sensors {
    ACCELEROMETER,
    GYROSCOPE,
    MOTION,
}

internal fun String.toSensor(): Sensors? {
    return when (this) {
        "accelerometer" -> Sensors.ACCELEROMETER
        "gyroscope" -> Sensors.GYROSCOPE
        "motion" -> Sensors.MOTION
        else -> {
            Log.e(
                "VKCaptchaWebView", "Incorrect or unsupported sensor type" +
                        "\n Sensor: $this"
            )
            null
        }
    }
}

internal fun JSONArray.toListOfSensors(): List<Sensors> {
    val sensors = ArrayList<Sensors>()
    for (i in 0 until this.length()) {
        this[i].toString().toSensor()?.let { sensors.add(it) }
    }
    return sensors
}

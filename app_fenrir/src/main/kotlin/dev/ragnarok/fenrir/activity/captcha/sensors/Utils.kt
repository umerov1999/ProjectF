package dev.ragnarok.fenrir.activity.captcha.sensors

import android.content.Context
import android.hardware.SensorManager

internal fun Context.getSensorManager(): SensorManager? =
    getSystemService(Context.SENSOR_SERVICE) as? SensorManager

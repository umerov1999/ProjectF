package dev.ragnarok.fenrir.activity.captcha.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.os.Handler
import dev.ragnarok.fenrir.activity.captcha.sensors.model.PeriodMs
import dev.ragnarok.fenrir.activity.captcha.sensors.model.PeriodMs.Companion.toUs
import dev.ragnarok.fenrir.activity.captcha.sensors.model.SensorData

internal val provider by lazy {
    HandlerThreadProvider("vk-sensor-thread")
}

internal class AndroidSensorListener<T : SensorData>(
    context: Context,
    private val handlerThreadProvider: HandlerThreadProvider = provider,
    private val sensorType: Int,
    private val toSensorData: (FloatArray) -> T,
) : BaseSensorListener<T>, SensorEventListener {

    private var onSensorChangedListener: (T) -> Unit = {}
    private val sensorManager = context.getSensorManager()
    private val sensor = sensorManager?.getDefaultSensor(sensorType)

    override fun startListening(periodMs: PeriodMs) {
        sensor?.let { sensor ->
            sensorManager?.registerListener(
                this,
                sensor,
                periodMs.toUs().value,
                Handler(handlerThreadProvider.provide().looper)
            )
        }
    }

    override fun stopListening() {
        sensorManager?.unregisterListener(this)
        provider.release()
    }

    override fun setOnSensorChangedListener(listener: (T) -> Unit) {
        onSensorChangedListener = listener
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == sensorType) {
            val sensorData = toSensorData(event.values)
            onSensorChangedListener(sensorData)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

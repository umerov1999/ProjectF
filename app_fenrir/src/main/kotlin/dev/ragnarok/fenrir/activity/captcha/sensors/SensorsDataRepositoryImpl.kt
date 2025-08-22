package dev.ragnarok.fenrir.activity.captcha.sensors

import android.content.Context
import android.hardware.Sensor
import android.os.Handler
import dev.ragnarok.fenrir.activity.captcha.di.DI.Companion.di
import dev.ragnarok.fenrir.activity.captcha.sensors.model.PeriodMs
import dev.ragnarok.fenrir.activity.captcha.sensors.model.SensorData
import dev.ragnarok.fenrir.activity.captcha.sensors.model.Sensors
import dev.ragnarok.fenrir.activity.captcha.sensors.model.SensorsData

internal class SensorsDataRepositoryImpl private constructor(
    private val handler: Handler,
    private val accelerometer: BaseSensorListener<SensorData.AccelerometerSensorData>,
    private val gyroscope: BaseSensorListener<SensorData.GyroscopeSensorData>,
    private val motion: BaseSensorListener<SensorData.MotionSensorData>,
) : SensorsDataRepository {

    private var currentAccelerometerSensorData: SensorData.AccelerometerSensorData? = null
        set(value) {
            field = value
            accelerometerChanged = true
        }
    private var currentGyroscopeSensorData: SensorData.GyroscopeSensorData? = null
        set(value) {
            field = value
            gyroscopeChanged = true
        }
    private var currentMotionSensorData: SensorData.MotionSensorData? = null
        set(value) {
            field = value
            motionChanged = true
        }

    private var currentSensors = emptyList<Sensors>()

    private var accelerometerChanged: Boolean = false
    private var gyroscopeChanged: Boolean = false
    private var motionChanged: Boolean = false

    override fun startListening(
        sensors: List<Sensors>,
        periodMs: PeriodMs,
        onDataUpdate: (SensorsData) -> Unit
    ) {
        currentSensors = sensors
        for (sensor in currentSensors) {
            when (sensor) {
                Sensors.ACCELEROMETER -> {
                    accelerometer.startListening(periodMs)
                    accelerometer.setOnSensorChangedListener {
                        currentAccelerometerSensorData = it
                        onNewData(onDataUpdate)
                    }
                }

                Sensors.GYROSCOPE -> {
                    gyroscope.startListening(periodMs)
                    gyroscope.setOnSensorChangedListener {
                        currentGyroscopeSensorData = it
                        onNewData(onDataUpdate)
                    }
                }

                Sensors.MOTION -> {
                    motion.startListening(periodMs)
                    motion.setOnSensorChangedListener {
                        currentMotionSensorData = it
                        onNewData(onDataUpdate)
                    }
                }
            }
        }
    }

    override fun stopListening() {
        accelerometer.stopListening()
        gyroscope.stopListening()
        motion.stopListening()
    }

    private fun onNewData(onDataUpdate: (SensorsData) -> Unit) {
        val accelerometerSensorData = currentAccelerometerSensorData
        val gyroscopeSensorData = currentGyroscopeSensorData
        val motionSensorData = currentMotionSensorData
        val currentData = ArrayList<SensorData>()
        for (sensor in currentSensors) {
            when (sensor) {
                Sensors.ACCELEROMETER -> {
                    if (accelerometerSensorData != null && accelerometerChanged) {
                        currentData.add(accelerometerSensorData)
                    }
                }

                Sensors.GYROSCOPE -> {
                    if (gyroscopeSensorData != null && gyroscopeChanged) {
                        currentData.add(gyroscopeSensorData)
                    }
                }

                Sensors.MOTION -> {
                    if (motionSensorData != null && motionChanged) {
                        currentData.add(motionSensorData)
                    }
                }
            }
        }

        if (currentData.size == currentSensors.size) {
            handler.post {
                onDataUpdate(currentData)
            }
            for (sensor in currentSensors) {
                when (sensor) {
                    Sensors.ACCELEROMETER -> accelerometerChanged = false
                    Sensors.GYROSCOPE -> gyroscopeChanged = false
                    Sensors.MOTION -> motionChanged = false
                }
            }
        }
    }

    companion object {
        internal fun create(context: Context): SensorsDataRepositoryImpl {
            val accelerometer = AndroidSensorListener(
                context = context,
                sensorType = Sensor.TYPE_ACCELEROMETER,
                toSensorData = SensorData.AccelerometerSensorData::toData
            )
            val gyroscope = AndroidSensorListener(
                context = context,
                sensorType = Sensor.TYPE_GYROSCOPE,
                toSensorData = SensorData.GyroscopeSensorData::toData
            )
            val motion = AndroidSensorListener(
                context = context,
                sensorType = Sensor.TYPE_ROTATION_VECTOR,
                toSensorData = SensorData.MotionSensorData::toData
            )

            return SensorsDataRepositoryImpl(
                Handler(di.mainLooper), accelerometer, gyroscope, motion
            )
        }
    }
}

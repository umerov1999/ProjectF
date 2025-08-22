package dev.ragnarok.fenrir.activity.captcha.sensors.model

import org.json.JSONObject

internal sealed class SensorData {

    abstract val sensorName: String

    abstract fun toJson(): JSONObject

    data class MotionSensorData(
        val alpha: Float,
        val beta: Float,
        val gamma: Float,
    ) : SensorData() {

        override val sensorName = "motion"

        override fun toJson(): JSONObject = JSONObject().apply {
            put("alpha", alpha)
            put("beta", beta)
            put("gamma", gamma)
        }

        companion object {

            fun toData(array: FloatArray): MotionSensorData {
                return MotionSensorData(
                    alpha = array[0],
                    beta = array[0],
                    gamma = array[0],
                )
            }
        }
    }

    data class AccelerometerSensorData(
        val x: Float,
        val y: Float,
        val z: Float,
    ) : SensorData() {

        override val sensorName = "accelerometer"

        override fun toJson(): JSONObject = JSONObject().apply {
            put("x", x)
            put("y", y)
            put("z", z)
        }

        companion object {

            fun toData(array: FloatArray): AccelerometerSensorData {
                return AccelerometerSensorData(
                    x = array[0],
                    y = array[1],
                    z = array[2],
                )
            }
        }
    }

    data class GyroscopeSensorData(
        val x: Float,
        val y: Float,
        val z: Float,
    ) : SensorData() {

        override val sensorName = "gyroscope"

        override fun toJson(): JSONObject = JSONObject().apply {
            put("x", x)
            put("y", y)
            put("z", z)
        }

        companion object {

            fun toData(array: FloatArray): GyroscopeSensorData {
                return GyroscopeSensorData(
                    x = array[0],
                    y = array[1],
                    z = array[2],
                )
            }
        }
    }
}

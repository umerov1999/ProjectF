package dev.ragnarok.fenrir.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKPushRegistration {
    @SerialName("userId")
    var userId = 0L
        private set

    @SerialName("deviceId")
    lateinit var deviceId: String
        private set

    @SerialName("vkToken")
    lateinit var vkToken: String
        private set

    @SerialName("gmcToken")
    lateinit var fcmToken: String
        private set

    operator fun set(
        userId: Long,
        deviceId: String,
        vkToken: String,
        fcmToken: String
    ): VKPushRegistration {
        this.userId = userId
        this.deviceId = deviceId
        this.vkToken = vkToken
        this.fcmToken = fcmToken
        return this
    }
}
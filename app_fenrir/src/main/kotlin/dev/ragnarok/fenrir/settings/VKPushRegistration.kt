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

    @SerialName("vkAccessToken")
    lateinit var vkAccessToken: String
        private set

    @SerialName("fcmToken")
    lateinit var fcmToken: String
        private set

    operator fun set(
        userId: Long,
        deviceId: String,
        vkAccessToken: String,
        fcmToken: String
    ): VKPushRegistration {
        this.userId = userId
        this.deviceId = deviceId
        this.vkAccessToken = vkAccessToken
        this.fcmToken = fcmToken
        return this
    }
}
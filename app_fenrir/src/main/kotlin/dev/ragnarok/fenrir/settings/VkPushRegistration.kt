package dev.ragnarok.fenrir.settings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VkPushRegistration {
    @SerialName("userId")
    var userId = 0
        private set

    @SerialName("deviceId")
    lateinit var deviceId: String
        private set

    @SerialName("vkToken")
    lateinit var vkToken: String
        private set

    @SerialName("gmcToken")
    lateinit var gmcToken: String
        private set

    operator fun set(
        userId: Int,
        deviceId: String,
        vkToken: String,
        gmcToken: String
    ): VkPushRegistration {
        this.userId = userId
        this.deviceId = deviceId
        this.vkToken = vkToken
        this.gmcToken = gmcToken
        return this
    }
}
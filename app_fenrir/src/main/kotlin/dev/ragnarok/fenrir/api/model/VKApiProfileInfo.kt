package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiProfileInfo {
    @SerialName("first_name")
    var first_name: String? = null

    @SerialName("last_name")
    var last_name: String? = null

    @SerialName("maiden_name")
    var maiden_name: String? = null

    @SerialName("screen_name")
    var screen_name: String? = null

    @SerialName("home_town")
    var home_town: String? = null

    @SerialName("bdate")
    var bdate: String? = null

    @SerialName("sex")
    var sex = 0
}
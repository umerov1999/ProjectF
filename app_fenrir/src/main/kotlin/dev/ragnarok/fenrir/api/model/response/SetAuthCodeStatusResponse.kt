package dev.ragnarok.fenrir.api.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SetAuthCodeStatusResponse {
    @SerialName("domain")
    var domain: String? = null

    @SerialName("expires_in")
    var expires_in: Long = 0

    @SerialName("faq_url")
    var faq_url: String? = null

    @SerialName("polling_delay")
    var polling_delay: Int = 0

    @SerialName("status")
    var status: Int = 0
}

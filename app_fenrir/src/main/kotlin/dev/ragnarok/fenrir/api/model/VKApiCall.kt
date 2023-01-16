package dev.ragnarok.fenrir.api.model

import dev.ragnarok.fenrir.api.model.interfaces.VKApiAttachment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiCall : VKApiAttachment {
    @SerialName("initiator_id")
    var initiator_id = 0L

    @SerialName("receiver_id")
    var receiver_id = 0L

    @SerialName("state")
    var state: String? = null

    @SerialName("time")
    var time: Long = 0
    override fun getType(): String {
        return VKApiAttachment.TYPE_CALL
    }
}
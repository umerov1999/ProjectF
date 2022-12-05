package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiGroupChats {
    @SerialName("id")
    var id = 0

    @SerialName("members_count")
    var members_count = 0

    @SerialName("last_message_date")
    var last_message_date: Long = 0

    @SerialName("is_closed")
    var is_closed = false

    @SerialName("invite_link")
    var invite_link: String? = null

    @SerialName("photo")
    var photo: String? = null

    @SerialName("title")
    var title: String? = null
}
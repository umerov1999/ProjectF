package dev.ragnarok.fenrir.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VKApiConversationMembers {
    @SerialName("member_id")
    var member_id = 0L

    @SerialName("invited_by")
    var invited_by = 0L

    @SerialName("join_date")
    var join_date: Long = 0

    @SerialName("is_admin")
    var is_admin = false

    @SerialName("is_owner")
    var is_owner = false

    @SerialName("can_kick")
    var can_kick = false
}
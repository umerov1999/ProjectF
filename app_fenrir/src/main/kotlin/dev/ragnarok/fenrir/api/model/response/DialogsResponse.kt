package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiConversation
import dev.ragnarok.fenrir.api.model.VKApiDialog
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DialogsResponse {
    @SerialName("items")
    var dialogs: ArrayList<VKApiDialog>? = null

    @SerialName("count")
    var count = 0

    @SerialName("unread_count")
    var unreadCount = 0

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null

    @SerialName("groups")
    var groups: List<VKApiCommunity>? = null

    @SerialName("contacts")
    var contacts: List<VKApiConversation.ContactElement>? = null
}
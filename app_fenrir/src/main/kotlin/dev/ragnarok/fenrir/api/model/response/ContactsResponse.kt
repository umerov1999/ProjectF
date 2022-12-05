package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiConversation
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ContactsResponse {
    @SerialName("items")
    var items: List<Int>? = null

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null

    @SerialName("contacts")
    var contacts: List<VKApiConversation.ContactElement>? = null
}
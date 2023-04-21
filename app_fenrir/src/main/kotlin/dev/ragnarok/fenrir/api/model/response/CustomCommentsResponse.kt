package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.adapters.CustomCommentsResponseDtoAdapter
import dev.ragnarok.fenrir.api.model.VKApiComment
import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiPoll
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable(with = CustomCommentsResponseDtoAdapter::class)
class CustomCommentsResponse {
    // Parse manually in CustomCommentsResponseAdapter
    var main: Main? = null
    var firstId: Int? = null
    var lastId: Int? = null
    var admin_level: Int? = null

    @Serializable
    class Main {
        @SerialName("count")
        var count = 0

        @SerialName("items")
        var comments: List<VKApiComment>? = null

        @SerialName("profiles")
        var profiles: List<VKApiUser>? = null

        @SerialName("groups")
        var groups: List<VKApiCommunity>? = null

        @SerialName("poll")
        var poll: VKApiPoll? = null
    }
}
package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.model.VKApiCommunity
import dev.ragnarok.fenrir.api.model.VKApiPost
import dev.ragnarok.fenrir.api.model.VKApiUser
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class NewsfeedSearchResponse {
    @SerialName("items")
    var items: List<VKApiPost>? = null

    @SerialName("profiles")
    var profiles: List<VKApiUser>? = null

    @SerialName("groups")
    var groups: List<VKApiCommunity>? = null

    @SerialName("next_from")
    var nextFrom: String? = null //@SerialName("count")
    //public Integer count;
    //@SerialName("total_count")
    //public Integer totalCount;
}
package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.adapters.LikesListAdapter
import dev.ragnarok.fenrir.api.model.VKApiOwner
import kotlinx.serialization.Serializable

@Serializable(with = LikesListAdapter::class)
class LikesListResponse {
    var count = 0
    var owners: ArrayList<VKApiOwner>? = null
}
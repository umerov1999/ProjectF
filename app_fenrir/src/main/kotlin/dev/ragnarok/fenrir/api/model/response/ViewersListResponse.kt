package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.adapters.ViewersListDtoAdapter
import dev.ragnarok.fenrir.api.model.VKApiOwner
import kotlinx.serialization.Serializable

@Serializable(with = ViewersListDtoAdapter::class)
class ViewersListResponse {
    var count = 0
    var ownersWithLikes: ArrayList<Pair<VKApiOwner, Boolean>>? = null
}
package dev.ragnarok.fenrir.api.model.response

import dev.ragnarok.fenrir.api.adapters.ViewersListAdapter
import dev.ragnarok.fenrir.api.model.VKApiOwner
import kotlinx.serialization.Serializable

@Serializable(with = ViewersListAdapter::class)
class ViewersListResponse {
    var count = 0
    var owners: ArrayList<VKApiOwner>? = null
}
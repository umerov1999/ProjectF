package dev.ragnarok.fenrir.fragment.friends.followers

import dev.ragnarok.fenrir.fragment.absownerslist.ISimpleOwnersView
import dev.ragnarok.fenrir.model.Owner

interface IFollowersView : ISimpleOwnersView {
    fun notifyRemoved(position: Int)
    fun showModFollowers(add: List<Owner>, remove: List<Owner>, accountId: Int, ownerId: Int)
}
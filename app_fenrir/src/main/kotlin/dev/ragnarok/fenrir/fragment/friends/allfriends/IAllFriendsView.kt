package dev.ragnarok.fenrir.fragment.friends.allfriends

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UsersPart

interface IAllFriendsView : IMvpView, IErrorView {
    fun notifyDatasetChanged(grouping: Boolean)
    fun setSwipeRefreshEnabled(enabled: Boolean)
    fun displayData(data: List<UsersPart>, grouping: Boolean)
    fun notifyItemRangeInserted(position: Int, count: Int)
    fun showUserWall(accountId: Long, user: User)
    fun showRefreshing(refreshing: Boolean)
    fun showModFriends(add: List<Owner>, remove: List<Owner>, accountId: Long, ownerId: Long)
}
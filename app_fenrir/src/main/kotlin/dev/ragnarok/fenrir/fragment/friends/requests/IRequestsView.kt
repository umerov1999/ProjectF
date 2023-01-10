package dev.ragnarok.fenrir.fragment.friends.requests

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.User
import dev.ragnarok.fenrir.model.UsersPart

interface IRequestsView : IMvpView, IErrorView {
    fun notifyDatasetChanged(grouping: Boolean)
    fun setSwipeRefreshEnabled(enabled: Boolean)
    fun displayData(data: List<UsersPart>, grouping: Boolean)
    fun notifyItemRangeInserted(position: Int, count: Int)
    fun showUserWall(accountId: Int, user: User)
    fun showRefreshing(refreshing: Boolean)
    fun showNotRequests(data: List<Owner>, accountId: Int, ownerId: Int)
    fun updateCount(count: Int)
}
package dev.ragnarok.fenrir.fragment.communities.communitycontrol.communitymembers

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Owner

interface ICommunityMembersView : IMvpView, IErrorView {
    fun displayData(users: List<Owner>)
    fun notifyItemRemoved(position: Int)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun openUserWall(accountId: Long, user: Owner)
    fun showRefreshing(refreshing: Boolean)
    fun onSearch(accountId: Long, groupId: Long)
    fun onOptions(filter: String?)
    fun showModMembers(add: List<Owner>, remove: List<Owner>, accountId: Long, ownerId: Long)
}
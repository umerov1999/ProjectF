package dev.ragnarok.fenrir.fragment.communities.communitycontrol.communitymanagers

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Manager
import dev.ragnarok.fenrir.model.User

interface ICommunityManagersView : IErrorView, IMvpView, IToastView {
    fun notifyDataSetChanged()
    fun displayRefreshing(loadingNow: Boolean)
    fun displayData(managers: List<Manager>)
    fun goToManagerEditing(accountId: Long, groupId: Long, manager: Manager)
    fun showUserProfile(accountId: Long, user: User)
    fun startSelectProfilesActivity(accountId: Long, groupId: Long)
    fun startAddingUsersToManagers(accountId: Long, groupId: Long, users: ArrayList<User>)
    fun notifyItemRemoved(index: Int)
    fun notifyItemChanged(index: Int)
    fun notifyItemAdded(index: Int)
}
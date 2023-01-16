package dev.ragnarok.fenrir.fragment.communitycontrol.communityblacklist

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Banned
import dev.ragnarok.fenrir.model.User

interface ICommunityBlacklistView : IErrorView, IMvpView, IToastView {
    fun displayRefreshing(loadingNow: Boolean)
    fun notifyDataSetChanged()
    fun diplayData(data: List<Banned>)
    fun notifyItemRemoved(index: Int)
    fun openBanEditor(accountId: Long, groupId: Long, banned: Banned)
    fun startSelectProfilesActivity(accountId: Long, groupId: Long)
    fun addUsersToBan(accountId: Long, groupId: Long, users: ArrayList<User>)
    fun notifyItemsAdded(position: Int, size: Int)
}
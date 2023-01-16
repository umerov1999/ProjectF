package dev.ragnarok.fenrir.fragment.friends.friendstabs

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.FriendsCounters

interface IFriendsTabsView : IMvpView, IErrorView {
    fun displayCounters(counters: FriendsCounters)
    fun configTabs(accountId: Long, userId: Long, isNotMyPage: Boolean)
    fun displayUserNameAtToolbar(userName: String?)
    fun setDrawerFriendsSectionSelected(selected: Boolean)
    fun onFriendsBirthday(accountId: Long, ownerId: Long)
}
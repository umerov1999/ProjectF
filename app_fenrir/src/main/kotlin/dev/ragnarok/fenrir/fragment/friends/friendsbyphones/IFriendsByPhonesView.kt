package dev.ragnarok.fenrir.fragment.friends.friendsbyphones

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.ContactConversation

interface IFriendsByPhonesView : IMvpView, IErrorView, IToastView {
    fun displayData(owners: List<ContactConversation>)
    fun notifyDataAdded(position: Int, count: Int)
    fun displayLoading(loading: Boolean)
    fun notifyDataSetChanged()
    fun showChat(accountId: Int, owner: ContactConversation)
}
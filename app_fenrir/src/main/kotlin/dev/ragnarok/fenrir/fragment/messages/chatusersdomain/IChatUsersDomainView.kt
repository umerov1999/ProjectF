package dev.ragnarok.fenrir.fragment.messages.chatusersdomain

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.AppChatUser
import dev.ragnarok.fenrir.model.Owner

interface IChatUsersDomainView : IMvpView, IErrorView {
    fun displayData(users: List<AppChatUser>)
    fun notifyItemRemoved(position: Int)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun openUserWall(accountId: Int, user: Owner)
    fun addDomain(accountId: Int, user: Owner)
    fun displayRefreshing(refreshing: Boolean)
}
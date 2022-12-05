package dev.ragnarok.fenrir.fragment.groupchats

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.GroupChats
import dev.ragnarok.fenrir.model.LoadMoreState

interface IGroupChatsView : IMvpView, IErrorView {
    fun displayData(chats: MutableList<GroupChats>)
    fun notifyDataSetChanged()
    fun notifyDataAdd(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun setupLoadMore(@LoadMoreState state: Int)
    fun goToChat(accountId: Int, chat_id: Int)
}
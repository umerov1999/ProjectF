package dev.ragnarok.fenrir.fragment.messages.importantmessages

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.messages.IBasicMessageListView
import dev.ragnarok.fenrir.model.Message

interface IImportantMessagesView : IBasicMessageListView, IErrorView {
    fun showRefreshing(refreshing: Boolean)
    fun notifyDataAdded(position: Int, count: Int)
    fun forwardMessages(accountId: Long, messages: ArrayList<Message>)
    fun goToMessagesLookup(accountId: Long, peerId: Long, messageId: Int)
}
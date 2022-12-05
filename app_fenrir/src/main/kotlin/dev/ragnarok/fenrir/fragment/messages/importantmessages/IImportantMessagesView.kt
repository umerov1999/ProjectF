package dev.ragnarok.fenrir.fragment.messages.importantmessages

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.messages.IBasicMessageListView
import dev.ragnarok.fenrir.model.Message

interface IImportantMessagesView : IBasicMessageListView, IErrorView {
    fun showRefreshing(refreshing: Boolean)
    fun notifyDataAdded(position: Int, count: Int)
    fun forwardMessages(accountId: Int, messages: ArrayList<Message>)
    fun goToMessagesLookup(accountId: Int, peerId: Int, messageId: Int)
}
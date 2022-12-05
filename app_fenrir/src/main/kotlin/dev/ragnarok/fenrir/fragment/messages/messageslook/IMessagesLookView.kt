package dev.ragnarok.fenrir.fragment.messages.messageslook

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.messages.IBasicMessageListView
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Peer

interface IMessagesLookView : IBasicMessageListView, IErrorView {
    fun focusTo(index: Int)
    fun setupHeaders(@LoadMoreState upHeaderState: Int, @LoadMoreState downHeaderState: Int)
    fun forwardMessages(accountId: Int, messages: ArrayList<Message>)
    fun displayToolbarAvatar(accountId: Int, peer: Peer)
}
package dev.ragnarok.fenrir.fragment.messages.localjsontochat

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Message
import dev.ragnarok.fenrir.model.Peer

interface ILocalJsonToChatView : IMvpView, IErrorView,
    IAttachmentsPlacesView {
    fun displayData(posts: ArrayList<Message>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun setToolbarTitle(title: String?)
    fun setToolbarSubtitle(subtitle: String?)
    fun scroll_pos(pos: Int)
    fun displayToolbarAvatar(peer: Peer)
    fun attachments_mode(accountId: Int, last_selected: Int)
    fun resolveEmptyText(visible: Boolean)
}
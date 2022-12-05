package dev.ragnarok.fenrir.fragment.conversation.abschatattachments

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView

interface IBaseChatAttachmentsView<T> : IMvpView, IAttachmentsPlacesView,
    IErrorView {
    fun displayAttachments(data: MutableList<T>)
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyDatasetChanged()
    fun showLoading(loading: Boolean)
    fun setEmptyTextVisible(visible: Boolean)
    fun setToolbarTitleString(title: String)
    fun setToolbarSubtitleString(subtitle: String)
}
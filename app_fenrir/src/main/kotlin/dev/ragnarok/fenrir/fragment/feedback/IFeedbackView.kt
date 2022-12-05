package dev.ragnarok.fenrir.fragment.feedback

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.feedback.Feedback

interface IFeedbackView : IMvpView, IAttachmentsPlacesView, IErrorView {
    fun displayData(data: MutableList<Feedback>)
    fun showLoading(loading: Boolean)
    fun notifyDataAdding(position: Int, count: Int)
    fun notifyFirstListReceived()
    fun notifyDataSetChanged()
    fun configLoadMore(@LoadMoreState loadmoreState: Int)
    fun showLinksDialog(accountId: Int, notification: Feedback)
}
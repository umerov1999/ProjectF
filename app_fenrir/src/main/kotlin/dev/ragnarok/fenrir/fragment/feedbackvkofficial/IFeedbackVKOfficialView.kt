package dev.ragnarok.fenrir.fragment.feedbackvkofficial

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.FeedbackVKOfficialList

interface IFeedbackVKOfficialView : IMvpView, IErrorView {
    fun displayData(pages: FeedbackVKOfficialList)
    fun notifyFirstListReceived()
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyItemRemoved(position: Int)
    fun showRefreshing(refreshing: Boolean)
}
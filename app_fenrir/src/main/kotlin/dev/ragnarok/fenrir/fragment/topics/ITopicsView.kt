package dev.ragnarok.fenrir.fragment.topics

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.Topic

interface ITopicsView : IMvpView, IErrorView {
    fun displayData(topics: MutableList<Topic>)
    fun notifyDataSetChanged()
    fun notifyDataAdd(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun setupLoadMore(@LoadMoreState state: Int)
    fun goToComments(accountId: Long, topic: Topic)
}
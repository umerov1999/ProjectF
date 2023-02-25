package dev.ragnarok.fenrir.fragment.feed

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.FeedSource
import dev.ragnarok.fenrir.model.LoadMoreState
import dev.ragnarok.fenrir.model.News

interface IFeedView : IAttachmentsPlacesView, IMvpView, IErrorView {
    fun displayFeedSources(sources: MutableList<FeedSource>)
    fun notifyFeedSourcesChanged()
    fun displayFeed(data: MutableList<News>, rawScrollState: String?)
    fun notifyFeedDataChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyItemChanged(position: Int)
    fun setupLoadMoreFooter(@LoadMoreState state: Int)
    fun showRefreshing(refreshing: Boolean)
    fun scrollFeedSourcesToPosition(position: Int)
    fun scrollTo(pos: Int)
    override fun goToLikes(accountId: Long, type: String, ownerId: Long, id: Int)
    override fun goToReposts(accountId: Long, type: String, ownerId: Long, id: Int)
    fun goToPostComments(accountId: Long, postId: Int, ownerId: Long)
    fun showSuccessToast()
    fun askToReload()
}
package dev.ragnarok.fenrir.fragment.wallattachments.wallpostqueryattachments

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Post

interface IWallPostQueryAttachmentsView : IMvpView, IErrorView,
    IAttachmentsPlacesView {
    fun displayData(posts: MutableList<Post>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun toolbarTitle(title: String)
    fun toolbarSubtitle(subtitle: String)
    fun onSetLoadingStatus(isLoad: Int)
    fun openPostEditor(accountId: Long, post: Post)
}
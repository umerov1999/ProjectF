package dev.ragnarok.fenrir.fragment.wallattachments.wallsearchcommentsattachments

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Comment

interface IWallSearchCommentsAttachmentsView : IMvpView, IErrorView,
    IAttachmentsPlacesView {
    fun displayData(comments: MutableList<Comment>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun toolbarTitle(title: String)
    fun toolbarSubtitle(subtitle: String)
    fun onSetLoadingStatus(isLoad: Int)
    fun moveFocusTo(index: Int)
    fun notifyItemChanged(index: Int)
    fun goToPost(accountId: Long, ownerId: Long, postId: Int)
}
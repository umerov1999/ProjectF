package dev.ragnarok.fenrir.fragment.fave.faveposts

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Post

interface IFavePostsView : IMvpView, IErrorView, IAttachmentsPlacesView {
    fun displayData(posts: MutableList<Post>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun notifyItemChanged(index: Int)
}
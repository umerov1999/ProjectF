package dev.ragnarok.fenrir.fragment.fave.favepages

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.FavePage
import dev.ragnarok.fenrir.model.Owner

interface IFavePagesView : IMvpView, IErrorView {
    fun displayData(pages: List<FavePage>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun openOwnerWall(accountId: Int, owner: Owner)
    fun openMention(accountId: Int, owner: Owner)
    fun notifyItemRemoved(index: Int)
}

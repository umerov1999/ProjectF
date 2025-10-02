package dev.ragnarok.fenrir.fragment.feed.ownerlist.owners

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Owner

interface IFeedOwnersView : IMvpView,
    IErrorView, IToastView {
    fun displayData(data: List<Owner>)
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyDataRemoved(position: Int, count: Int)
    fun notifyDataChanged(position: Int, count: Int)
    fun notifyDataSetChanged()
    fun showLoading(loading: Boolean)

    fun onOpenWall(accountId: Long, ownerId: Long)
}

package dev.ragnarok.fenrir.fragment.feed.ownerlist

import dev.ragnarok.fenrir.db.model.entity.FeedOwnersEntity
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView

interface IFeedOwnerListView : IMvpView,
    IErrorView, IToastView {
    fun displayData(data: List<FeedOwnersEntity>)
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyDataRemoved(position: Int, count: Int)
    fun notifyDataChanged(position: Int, count: Int)
    fun notifyDataSetChanged()
    fun showLoading(loading: Boolean)
    fun feedOwnerListOpen(accountId: Long, listId: Long)
}

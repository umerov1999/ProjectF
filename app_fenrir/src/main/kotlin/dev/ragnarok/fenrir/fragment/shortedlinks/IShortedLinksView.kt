package dev.ragnarok.fenrir.fragment.shortedlinks

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.ShortLink

interface IShortedLinksView : IMvpView, IErrorView {
    fun displayData(links: List<ShortLink>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun updateLink(url: String?)
    fun showLinkStatus(status: String?)
}
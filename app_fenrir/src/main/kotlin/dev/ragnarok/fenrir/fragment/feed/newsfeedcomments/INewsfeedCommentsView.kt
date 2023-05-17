package dev.ragnarok.fenrir.fragment.feed.newsfeedcomments

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.NewsfeedComment

interface INewsfeedCommentsView : IAttachmentsPlacesView, IMvpView,
    IErrorView {
    fun displayData(data: List<NewsfeedComment>)
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyDataSetChanged()
    fun showLoading(loading: Boolean)
}
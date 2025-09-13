package dev.ragnarok.fenrir.fragment.feed.newsfeedmentions

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.NewsfeedComment

interface INewsfeedMentionsView : IAttachmentsPlacesView, IMvpView,
    IErrorView {
    fun displayData(data: List<NewsfeedComment>)
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyDataSetChanged()
    fun showLoading(loading: Boolean)
}
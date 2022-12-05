package dev.ragnarok.fenrir.fragment.fave.favevideos

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Video

interface IFaveVideosView : IMvpView, IErrorView {
    fun displayData(videos: List<Video>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun goToPreview(accountId: Int, video: Video)
}
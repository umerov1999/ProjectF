package dev.ragnarok.fenrir.fragment.localserver.videoslocalserver

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Video

interface IVideosLocalServerView : IMvpView, IErrorView {
    fun displayList(videos: List<Video>)
    fun notifyListChanged()
    fun notifyItemChanged(index: Int)
    fun notifyDataAdded(position: Int, count: Int)
    fun displayLoading(loading: Boolean)
}
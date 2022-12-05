package dev.ragnarok.fenrir.fragment.videoalbumsbyvideo

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.VideoAlbum

interface IVideoAlbumsByVideoView : IMvpView, IErrorView {
    fun displayData(data: List<VideoAlbum>)
    fun notifyDataAdded(position: Int, count: Int)
    fun displayLoading(loading: Boolean)
    fun notifyDataSetChanged()
    fun openAlbum(accountId: Int, ownerId: Int, albumId: Int, action: String?, title: String?)
}
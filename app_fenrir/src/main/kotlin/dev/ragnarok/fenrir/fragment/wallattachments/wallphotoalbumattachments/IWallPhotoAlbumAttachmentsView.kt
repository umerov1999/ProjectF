package dev.ragnarok.fenrir.fragment.wallattachments.wallphotoalbumattachments

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.PhotoAlbum

interface IWallPhotoAlbumAttachmentsView : IMvpView, IErrorView,
    IAttachmentsPlacesView {
    fun displayData(albums: MutableList<PhotoAlbum>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun toolbarTitle(title: String)
    fun toolbarSubtitle(subtitle: String)
    fun onSetLoadingStatus(isLoad: Int)
}
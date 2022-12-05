package dev.ragnarok.fenrir.fragment.wallattachments.wallphotosattachments

import dev.ragnarok.fenrir.fragment.base.IAttachmentsPlacesView
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.TmpSource

interface IWallPhotosAttachmentsView : IMvpView, IErrorView,
    IAttachmentsPlacesView {
    fun displayData(photos: MutableList<Photo>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun toolbarTitle(title: String)
    fun toolbarSubtitle(subtitle: String)
    fun goToTempPhotosGallery(accountId: Int, source: TmpSource, index: Int)
    fun goToTempPhotosGallery(accountId: Int, ptr: Long, index: Int)
    fun onSetLoadingStatus(isLoad: Int)
}
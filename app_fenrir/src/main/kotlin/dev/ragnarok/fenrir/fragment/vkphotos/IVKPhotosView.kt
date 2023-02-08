package dev.ragnarok.fenrir.fragment.vkphotos

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToolbarView
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.TmpSource
import dev.ragnarok.fenrir.model.wrappers.SelectablePhotoWrapper
import dev.ragnarok.fenrir.upload.Upload

interface IVKPhotosView : IMvpView, IErrorView, IToolbarView {
    fun displayData(photos: List<SelectablePhotoWrapper>, uploads: List<Upload>)
    fun notifyDataSetChanged()
    fun notifyPhotosAdded(position: Int, count: Int)
    fun notifyPhotosChanged(position: Int, count: Int)
    fun displayRefreshing(refreshing: Boolean)
    fun notifyUploadAdded(position: Int, count: Int)
    fun notifyUploadRemoved(index: Int)
    fun setButtonAddVisible(visible: Boolean, anim: Boolean)
    fun notifyUploadItemChanged(index: Int)
    fun notifyUploadProgressChanged(id: Int, progress: Int)
    fun displayGallery(
        accountId: Long,
        albumId: Int,
        ownerId: Long,
        source: TmpSource,
        position: Int
    )

    fun displayGalleryUnSafe(
        accountId: Long,
        albumId: Int,
        ownerId: Long,
        parcelNativePointer: Long,
        position: Int
    )

    fun displayDefaultToolbarTitle()
    fun setDrawerPhotosSelected(selected: Boolean)
    fun returnSelectionToParent(selected: List<Photo>)
    fun showSelectPhotosToast()
    fun startLocalPhotosSelection()
    fun startLocalPhotosSelectionIfHasPermission()
    fun onToggleShowDate(isShow: Boolean)
    fun displayToolbarSubtitle(album: PhotoAlbum?, text: String)
    fun scrollTo(position: Int)

    companion object {
        const val ACTION_SHOW_PHOTOS = "dev.ragnarok.fenrir.ACTION_SHOW_PHOTOS"
        const val ACTION_SELECT_PHOTOS = "dev.ragnarok.fenrir.ACTION_SELECT_PHOTOS"
    }
}

package dev.ragnarok.fenrir.fragment.vkphotoalbums

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Owner
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoAlbumEditor

interface IPhotoAlbumsView : IMvpView, IErrorView {
    fun displayData(data: List<PhotoAlbum>)
    fun displayLoading(loading: Boolean)
    fun notifyDataSetChanged()
    fun setToolbarSubtitle(subtitle: String?)
    fun openAlbum(accountId: Long, album: PhotoAlbum, owner: Owner?, action: String?)
    fun showAlbumContextMenu(album: PhotoAlbum)
    fun showDeleteConfirmDialog(album: PhotoAlbum)
    fun doSelection(album: PhotoAlbum)
    fun setCreateAlbumFabVisible(visible: Boolean)
    fun goToAlbumCreation(accountId: Long, ownerId: Long)
    fun goToAlbumEditing(accountId: Long, album: PhotoAlbum, editor: PhotoAlbumEditor)
    fun setDrawerPhotoSectionActive(active: Boolean)
    fun notifyItemRemoved(index: Int)
    fun notifyItemChanged(index: Int)
    fun notifyDataAdded(position: Int, size: Int)
    fun goToPhotoComments(accountId: Long, ownerId: Long)
}
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
    fun openAlbum(accountId: Int, album: PhotoAlbum, owner: Owner?, action: String?)
    fun showAlbumContextMenu(album: PhotoAlbum)
    fun showDeleteConfirmDialog(album: PhotoAlbum)
    fun doSelection(album: PhotoAlbum)
    fun setCreateAlbumFabVisible(visible: Boolean)
    fun goToAlbumCreation(accountId: Int, ownerId: Int)
    fun goToAlbumEditing(accountId: Int, album: PhotoAlbum, editor: PhotoAlbumEditor)
    fun setDrawerPhotoSectionActive(active: Boolean)
    fun notifyItemRemoved(index: Int)
    fun notifyItemChanged(index: Int)
    fun notifyDataAdded(position: Int, size: Int)
    fun goToPhotoComments(accountId: Int, ownerId: Int)
}
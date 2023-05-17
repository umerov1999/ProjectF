package dev.ragnarok.fenrir.fragment.photos.localimagealbums

import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.LocalImageAlbum

interface ILocalPhotoAlbumsView : IMvpView {
    fun displayData(data: List<LocalImageAlbum>)
    fun setEmptyTextVisible(visible: Boolean)
    fun displayProgress(loading: Boolean)
    fun openAlbum(album: LocalImageAlbum)
    fun notifyDataChanged()
    fun requestReadExternalStoragePermission()
}
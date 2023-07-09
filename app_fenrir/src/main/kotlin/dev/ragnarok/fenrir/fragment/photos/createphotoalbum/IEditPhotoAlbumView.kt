package dev.ragnarok.fenrir.fragment.photos.createphotoalbum

import dev.ragnarok.fenrir.api.model.VKApiPhotoAlbum
import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.ISteppersView
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.view.steppers.impl.CreatePhotoAlbumStepsHost

interface IEditPhotoAlbumView : ISteppersView<CreatePhotoAlbumStepsHost>, IErrorView {
    fun goToAlbum(accountId: Long, album: VKApiPhotoAlbum)
    fun goToEditedAlbum(accountId: Long, album: PhotoAlbum?, ret: Boolean?)
}
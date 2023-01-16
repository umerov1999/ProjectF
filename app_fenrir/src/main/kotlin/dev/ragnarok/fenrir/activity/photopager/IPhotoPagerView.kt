package dev.ragnarok.fenrir.activity.photopager

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Commented
import dev.ragnarok.fenrir.model.Photo

interface IPhotoPagerView : IMvpView, IErrorView, IToastView {
    fun goToLikesList(accountId: Long, ownerId: Long, photoId: Int)
    fun setupLikeButton(visible: Boolean, like: Boolean, likes: Int)
    fun setupWithUserButton(users: Int)
    fun setupShareButton(visible: Boolean, reposts: Int)
    fun setupCommentsButton(visible: Boolean, count: Int)
    fun displayPhotos(photos: List<Photo>, initialIndex: Int)
    fun setToolbarTitle(title: String?)
    fun setToolbarSubtitle(subtitle: String?)
    fun sharePhoto(accountId: Long, photo: Photo)
    fun postToMyWall(photo: Photo, accountId: Long)
    fun requestWriteToExternalStoragePermission()
    fun setButtonRestoreVisible(visible: Boolean)
    fun setupOptionMenu(canSaveYourself: Boolean, canDelete: Boolean)
    fun goToComments(accountId: Long, commented: Commented)
    fun displayPhotoListLoading(loading: Boolean)
    fun setButtonsBarVisible(visible: Boolean)
    fun setToolbarVisible(visible: Boolean)
    fun rebindPhotoAt(position: Int)
    fun closeOnly()
    fun returnInfo(position: Int, parcelNativePtr: Long)
    fun returnOnlyPos(position: Int)
}
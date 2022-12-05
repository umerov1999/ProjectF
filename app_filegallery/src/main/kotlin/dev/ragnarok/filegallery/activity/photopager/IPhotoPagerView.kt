package dev.ragnarok.filegallery.activity.photopager

import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.fragment.base.core.IToastView
import dev.ragnarok.filegallery.model.Photo
import dev.ragnarok.filegallery.model.Video

interface IPhotoPagerView : IMvpView, IErrorView, IToastView {
    fun displayPhotos(photos: List<Photo>, initialIndex: Int)
    fun setToolbarTitle(title: String?)
    fun setToolbarSubtitle(subtitle: String?)
    fun displayPhotoListLoading(loading: Boolean)
    fun setButtonsBarVisible(visible: Boolean)
    fun setToolbarVisible(visible: Boolean)
    fun closeOnly()
    fun returnInfo(position: Int, parcelNativePtr: Long)
    fun returnOnlyPos(position: Int)
    fun returnFileInfo(path: String)
    fun displayVideo(video: Video)
}
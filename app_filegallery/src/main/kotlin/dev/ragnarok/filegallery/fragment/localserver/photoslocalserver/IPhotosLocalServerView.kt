package dev.ragnarok.filegallery.fragment.localserver.photoslocalserver

import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.model.Photo

interface IPhotosLocalServerView : IMvpView, IErrorView {
    fun displayList(photos: List<Photo>)
    fun notifyListChanged()
    fun notifyItemChanged(index: Int)
    fun notifyDataAdded(position: Int, count: Int)
    fun displayLoading(loading: Boolean)
    fun displayGalleryUnSafe(parcelNativePointer: Long, position: Int, reversed: Boolean)
    fun scrollTo(position: Int)
}
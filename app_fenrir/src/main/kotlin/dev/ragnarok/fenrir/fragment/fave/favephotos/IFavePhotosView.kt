package dev.ragnarok.fenrir.fragment.fave.favephotos

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Photo

interface IFavePhotosView : IMvpView, IErrorView {
    fun displayData(photos: List<Photo>)
    fun notifyDataSetChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun showRefreshing(refreshing: Boolean)
    fun goToGallery(accountId: Long, photos: ArrayList<Photo>, position: Int)
}
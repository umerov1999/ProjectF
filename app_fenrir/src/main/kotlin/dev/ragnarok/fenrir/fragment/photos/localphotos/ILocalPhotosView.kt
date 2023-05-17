package dev.ragnarok.fenrir.fragment.photos.localphotos

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.LocalPhoto

interface ILocalPhotosView : IMvpView, IErrorView {
    fun displayData(data: List<LocalPhoto>)
    fun setEmptyTextVisible(visible: Boolean)
    fun displayProgress(loading: Boolean)
    fun returnResultToParent(photos: ArrayList<LocalPhoto>)
    fun updateSelectionAndIndexes()
    fun setFabVisible(visible: Boolean, anim: Boolean)
    fun requestReadExternalStoragePermission()
}
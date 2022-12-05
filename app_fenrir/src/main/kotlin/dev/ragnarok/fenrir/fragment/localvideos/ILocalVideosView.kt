package dev.ragnarok.fenrir.fragment.localvideos

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.LocalVideo

interface ILocalVideosView : IMvpView, IErrorView {
    fun displayData(data: List<LocalVideo>)
    fun setEmptyTextVisible(visible: Boolean)
    fun displayProgress(loading: Boolean)
    fun returnResultToParent(videos: ArrayList<LocalVideo>)
    fun updateSelectionAndIndexes()
    fun setFabVisible(visible: Boolean, anim: Boolean)
}
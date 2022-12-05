package dev.ragnarok.filegallery.fragment.localserver.audioslocalserver

import dev.ragnarok.filegallery.fragment.base.core.IErrorView
import dev.ragnarok.filegallery.fragment.base.core.IMvpView
import dev.ragnarok.filegallery.model.Audio

interface IAudiosLocalServerView : IMvpView, IErrorView {
    fun displayList(audios: List<Audio>)
    fun notifyListChanged()
    fun notifyItemChanged(index: Int)
    fun notifyDataAdded(position: Int, count: Int)
    fun displayLoading(loading: Boolean)
    fun displayOptionsDialog(isReverse: Boolean, isDiscography: Boolean)
}
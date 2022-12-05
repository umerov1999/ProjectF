package dev.ragnarok.fenrir.fragment.localserver.audioslocalserver

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Audio

interface IAudiosLocalServerView : IMvpView, IErrorView {
    fun displayList(audios: List<Audio>)
    fun notifyListChanged()
    fun notifyItemChanged(index: Int)
    fun notifyDataAdded(position: Int, count: Int)
    fun displayLoading(loading: Boolean)
    fun displayOptionsDialog(isReverse: Boolean, isDiscography: Boolean)
}
package dev.ragnarok.fenrir.fragment.audio.audiosbyartist

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Audio

interface IAudiosByArtistView : IMvpView, IErrorView, IToastView {
    fun displayList(audios: MutableList<Audio>)
    fun notifyListChanged()
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyItemChanged(index: Int)
    fun displayRefreshing(refreshing: Boolean)
}
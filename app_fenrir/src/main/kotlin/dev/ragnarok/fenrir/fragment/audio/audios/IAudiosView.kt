package dev.ragnarok.fenrir.fragment.audio.audios

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.fragment.base.core.IToastView
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioPlaylist

interface IAudiosView : IMvpView, IErrorView, IToastView {
    fun displayList(audios: MutableList<Audio>)
    fun notifyListChanged()
    fun notifyItemMoved(fromPosition: Int, toPosition: Int)
    fun notifyItemRemoved(index: Int)
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyItemChanged(index: Int)
    fun displayRefreshing(refreshing: Boolean)
    fun updatePlaylists(playlist: MutableList<AudioPlaylist>)
    fun showAudioDeadHelper()
}
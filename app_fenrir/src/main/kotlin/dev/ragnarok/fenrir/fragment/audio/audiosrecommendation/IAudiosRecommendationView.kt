package dev.ragnarok.fenrir.fragment.audio.audiosrecommendation

import dev.ragnarok.fenrir.fragment.base.core.IErrorView
import dev.ragnarok.fenrir.fragment.base.core.IMvpView
import dev.ragnarok.fenrir.model.Audio

interface IAudiosRecommendationView : IMvpView, IErrorView {
    fun displayList(audios: MutableList<Audio>)
    fun notifyListChanged()
    fun notifyItemRemoved(index: Int)
    fun notifyDataAdded(position: Int, count: Int)
    fun notifyItemChanged(index: Int)
    fun displayRefreshing(refreshing: Boolean)
}
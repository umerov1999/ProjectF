package dev.ragnarok.fenrir.fragment.search.audiossearch

import dev.ragnarok.fenrir.fragment.search.abssearch.IBaseSearchView
import dev.ragnarok.fenrir.model.Audio

interface IAudioSearchView : IBaseSearchView<Audio> {
    fun notifyAudioChanged(index: Int)
}
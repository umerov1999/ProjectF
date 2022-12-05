package dev.ragnarok.fenrir.fragment.search.videosearch

import dev.ragnarok.fenrir.fragment.search.abssearch.IBaseSearchView
import dev.ragnarok.fenrir.model.Video

interface IVideosSearchView : IBaseSearchView<Video> {
    fun returnSelectionToParent(video: Video)
}
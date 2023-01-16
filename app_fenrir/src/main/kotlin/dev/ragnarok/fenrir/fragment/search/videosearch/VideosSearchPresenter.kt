package dev.ragnarok.fenrir.fragment.search.videosearch

import android.os.Bundle
import dev.ragnarok.fenrir.domain.IVideosInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.VideoSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.fragment.videos.IVideosListView
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import io.reactivex.rxjava3.core.Single

class VideosSearchPresenter(
    accountId: Long,
    criteria: VideoSearchCriteria?,
    private val action: String?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<IVideosSearchView, VideoSearchCriteria, Video, IntNextFrom>(
    accountId,
    criteria,
    savedInstanceState
) {
    private val videosInteractor: IVideosInteractor = InteractorFactory.createVideosInteractor()
    override val initialNextFrom: IntNextFrom
        get() = IntNextFrom(0)

    override fun readParcelSaved(savedInstanceState: Bundle, key: String): VideoSearchCriteria? {
        return savedInstanceState.getParcelableCompat(key)
    }

    override fun isAtLast(startFrom: IntNextFrom): Boolean {
        return startFrom.offset == 0
    }

    override fun doSearch(
        accountId: Long,
        criteria: VideoSearchCriteria,
        startFrom: IntNextFrom
    ): Single<Pair<List<Video>, IntNextFrom>> {
        val offset = startFrom.offset
        val nextFrom = IntNextFrom(offset + 50)
        return videosInteractor.search(accountId, criteria, 50, offset)
            .map { videos -> create(videos, nextFrom) }
    }

    override fun instantiateEmptyCriteria(): VideoSearchCriteria {
        return VideoSearchCriteria("", false, null)
    }

    override fun canSearch(criteria: VideoSearchCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty()
    }

    fun fireVideoClicked(apiVideo: Video) {
        if (IVideosListView.ACTION_SELECT.equals(action, ignoreCase = true)) {
            view?.returnSelectionToParent(
                apiVideo
            )
        } else {
            view?.openVideo(accountId, apiVideo)
        }
    }
}
package dev.ragnarok.fenrir.fragment.search.audioplaylistsearch

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.AudioPlaylistSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import dev.ragnarok.fenrir.util.coroutines.CoroutinesUtils.fromIOToMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AudioPlaylistSearchPresenter(
    accountId: Long,
    criteria: AudioPlaylistSearchCriteria?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<IAudioPlaylistSearchView, AudioPlaylistSearchCriteria, AudioPlaylist, IntNextFrom>(
    accountId,
    criteria,
    savedInstanceState
) {
    private val audioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
    override val initialNextFrom: IntNextFrom
        get() = IntNextFrom(0)

    override fun readParcelSaved(
        savedInstanceState: Bundle,
        key: String
    ): AudioPlaylistSearchCriteria? {
        return savedInstanceState.getParcelableCompat(key)
    }

    override fun isAtLast(startFrom: IntNextFrom): Boolean {
        return startFrom.offset == 0
    }

    override fun onSearchError(throwable: Throwable) {
        super.onSearchError(throwable)
        showError(getCauseIfRuntime(throwable))
    }

    override fun doSearch(
        accountId: Long,
        criteria: AudioPlaylistSearchCriteria,
        startFrom: IntNextFrom
    ): Flow<Pair<List<AudioPlaylist>, IntNextFrom>> {
        val nextFrom = IntNextFrom(startFrom.offset + 50)
        return audioInteractor.searchPlaylists(accountId, criteria, startFrom.offset, 50)
            .map { audio -> create(audio, nextFrom) }
    }

    fun onAdd(album: AudioPlaylist, clone: Boolean) {
        appendJob(
            (if (clone) audioInteractor.clonePlaylist(
                accountId,
                album.id,
                album.owner_id
            ) else audioInteractor.followPlaylist(
                accountId,
                album.id,
                album.owner_id,
                album.access_key
            ))
                .fromIOToMain({
                    view?.customToast?.showToast(R.string.success)
                }) { throwable ->
                    showError(throwable)
                })
    }

    override fun canSearch(criteria: AudioPlaylistSearchCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty()
    }

    override fun instantiateEmptyCriteria(): AudioPlaylistSearchCriteria {
        return AudioPlaylistSearchCriteria("")
    }

}
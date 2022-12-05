package dev.ragnarok.fenrir.fragment.search.audioplaylistsearch

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.AudioPlaylistSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import io.reactivex.rxjava3.core.Single

class AudioPlaylistSearchPresenter(
    accountId: Int,
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
        accountId: Int,
        criteria: AudioPlaylistSearchCriteria,
        startFrom: IntNextFrom
    ): Single<Pair<List<AudioPlaylist>, IntNextFrom>> {
        val nextFrom = IntNextFrom(startFrom.offset + 50)
        return audioInteractor.searchPlaylists(accountId, criteria, startFrom.offset, 50)
            .map { audio -> create(audio, nextFrom) }
    }

    fun onAdd(album: AudioPlaylist, clone: Boolean) {
        appendDisposable((if (clone) audioInteractor.clonePlaylist(
            accountId,
            album.getId(),
            album.getOwnerId()
        ) else audioInteractor.followPlaylist(
            accountId,
            album.getId(),
            album.getOwnerId(),
            album.getAccess_key()
        ))
            .fromIOToMain()
            .subscribe({
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
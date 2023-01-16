package dev.ragnarok.fenrir.fragment.search.artistsearch

import android.os.Bundle
import dev.ragnarok.fenrir.R
import dev.ragnarok.fenrir.api.model.VKApiArtist
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.ArtistSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.fromIOToMain
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import io.reactivex.rxjava3.core.Single

class ArtistSearchPresenter(
    accountId: Long,
    criteria: ArtistSearchCriteria?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<IArtistSearchView, ArtistSearchCriteria, VKApiArtist, IntNextFrom>(
    accountId,
    criteria,
    savedInstanceState
) {
    private val audioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
    override val initialNextFrom: IntNextFrom
        get() = IntNextFrom(0)

    override fun isAtLast(startFrom: IntNextFrom): Boolean {
        return startFrom.offset == 0
    }

    override fun onSearchError(throwable: Throwable) {
        super.onSearchError(throwable)
        showError(getCauseIfRuntime(throwable))
    }

    override fun doSearch(
        accountId: Long,
        criteria: ArtistSearchCriteria,
        startFrom: IntNextFrom
    ): Single<Pair<List<VKApiArtist>, IntNextFrom>> {
        val nextFrom = IntNextFrom(startFrom.offset + 50)
        return audioInteractor.searchArtists(accountId, criteria, startFrom.offset, 50)
            .map { audio -> create(audio, nextFrom) }
    }

    fun onAdd(album: AudioPlaylist) {
        appendDisposable(audioInteractor.followPlaylist(
            accountId,
            album.getId(),
            album.getOwnerId(),
            album.getAccess_key()
        )
            .fromIOToMain()
            .subscribe({
                view?.customToast?.showToast(R.string.success)
            }) { throwable ->
                showError(
                    throwable
                )
            })
    }

    override fun canSearch(criteria: ArtistSearchCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty()
    }

    override fun instantiateEmptyCriteria(): ArtistSearchCriteria {
        return ArtistSearchCriteria("")
    }

    override fun readParcelSaved(savedInstanceState: Bundle, key: String): ArtistSearchCriteria? {
        return savedInstanceState.getParcelableCompat(key)
    }
}
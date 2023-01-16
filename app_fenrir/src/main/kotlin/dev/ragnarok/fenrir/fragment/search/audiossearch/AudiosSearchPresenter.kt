package dev.ragnarok.fenrir.fragment.search.audiossearch

import android.content.Context
import android.os.Bundle
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.InteractorFactory
import dev.ragnarok.fenrir.fragment.search.abssearch.AbsSearchPresenter
import dev.ragnarok.fenrir.fragment.search.criteria.AudioSearchCriteria
import dev.ragnarok.fenrir.fragment.search.nextfrom.IntNextFrom
import dev.ragnarok.fenrir.getParcelableCompat
import dev.ragnarok.fenrir.media.music.MusicPlaybackService.Companion.startForPlayList
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.place.PlaceFactory.getPlayerPlace
import dev.ragnarok.fenrir.settings.Settings
import dev.ragnarok.fenrir.trimmedNonNullNoEmpty
import dev.ragnarok.fenrir.util.Pair
import dev.ragnarok.fenrir.util.Pair.Companion.create
import dev.ragnarok.fenrir.util.Utils.getCauseIfRuntime
import io.reactivex.rxjava3.core.Single

class AudiosSearchPresenter(
    accountId: Long,
    criteria: AudioSearchCriteria?,
    savedInstanceState: Bundle?
) : AbsSearchPresenter<IAudioSearchView, AudioSearchCriteria, Audio, IntNextFrom>(
    accountId,
    criteria,
    savedInstanceState
) {
    private val audioInteractor: IAudioInteractor = InteractorFactory.createAudioInteractor()
    override val initialNextFrom: IntNextFrom
        get() = IntNextFrom(0)

    override fun readParcelSaved(savedInstanceState: Bundle, key: String): AudioSearchCriteria? {
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
        criteria: AudioSearchCriteria,
        startFrom: IntNextFrom
    ): Single<Pair<List<Audio>, IntNextFrom>> {
        val nextFrom = IntNextFrom(startFrom.offset + 50)
        return audioInteractor.search(accountId, criteria, startFrom.offset, 50)
            .map { audio -> create(audio, nextFrom) }
    }

    fun playAudio(context: Context, position: Int) {
        startForPlayList(context, data as ArrayList<Audio>, position, false)
        if (!Settings.get().other().isShow_mini_player) getPlayerPlace(
            Settings.get().accounts().current
        ).tryOpenWith(
            context
        )
    }

    override fun canSearch(criteria: AudioSearchCriteria?): Boolean {
        return criteria?.query.trimmedNonNullNoEmpty()
    }

    override fun instantiateEmptyCriteria(): AudioSearchCriteria {
        return AudioSearchCriteria("", by_artist = false, in_main_page = false)
    }

    val selected: ArrayList<Audio>
        get() {
            val ret = ArrayList<Audio>()
            for (i in data) {
                if (i.isSelected) ret.add(i)
            }
            return ret
        }

    fun getAudioPos(audio: Audio?): Int {
        if (data.isNotEmpty() && audio != null) {
            for ((pos, i) in data.withIndex()) {
                if (i.id == audio.id && i.ownerId == audio.ownerId) {
                    i.isAnimationNow = true
                    view?.notifyAudioChanged(
                        pos
                    )
                    return pos
                }
            }
        }
        return -1
    }

}
package dev.ragnarok.fenrir.domain.impl

import android.annotation.SuppressLint
import dev.ragnarok.fenrir.api.interfaces.INetworker
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.ArtistInfo
import dev.ragnarok.fenrir.api.model.VKApiArtist
import dev.ragnarok.fenrir.api.model.response.AddToPlaylistResponse
import dev.ragnarok.fenrir.domain.IAudioInteractor
import dev.ragnarok.fenrir.domain.mappers.Dto2Model.transform
import dev.ragnarok.fenrir.fragment.search.criteria.ArtistSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.AudioPlaylistSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.AudioSearchCriteria
import dev.ragnarok.fenrir.fragment.search.options.SpinnerOption
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Block
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2List
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Section
import dev.ragnarok.fenrir.util.Utils.listEmptyIfNull
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

class AudioInteractor(private val networker: INetworker) : IAudioInteractor {
    override fun add(accountId: Long, orig: Audio, groupId: Long?): Completable {
        return networker.vkDefault(accountId)
            .audio()
            .add(orig.id, orig.ownerId, groupId, orig.accessKey)
            .ignoreElement()
    }

    override fun delete(accountId: Long, audioId: Int, ownerId: Long): Completable {
        return networker.vkDefault(accountId)
            .audio()
            .delete(audioId, ownerId)
            .ignoreElement()
    }

    override fun edit(
        accountId: Long,
        ownerId: Long,
        audioId: Int,
        artist: String?,
        title: String?,
        text: String?
    ): Completable {
        return networker.vkDefault(accountId)
            .audio()
            .edit(ownerId, audioId, artist, title, text)
            .ignoreElement()
    }

    override fun restore(accountId: Long, audioId: Int, ownerId: Long): Completable {
        return networker.vkDefault(accountId)
            .audio()
            .restore(audioId, ownerId)
            .ignoreElement()
    }

    override fun sendBroadcast(
        accountId: Long,
        audioOwnerId: Long,
        audioId: Int,
        accessKey: String?,
        targetIds: Collection<Long>
    ): Completable {
        return networker.vkDefault(accountId)
            .audio()
            .setBroadcast(AccessIdPair(audioId, audioOwnerId, accessKey), targetIds)
            .ignoreElement()
    }

    override fun get(
        accountId: Long,
        playlist_id: Int?,
        ownerId: Long,
        offset: Int,
        count: Int,
        accessKey: String?
    ): Single<List<Audio>> {
        return networker.vkDefault(accountId)
            .audio()[playlist_id, ownerId, offset, count, accessKey]
            .map {
                listEmptyIfNull(
                    it.items
                )
            }
            .map { out ->
                val ret: MutableList<Audio> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun getPlaylistsCustom(accountId: Long, code: String?): Single<List<AudioPlaylist>> {
        return networker.vkDefault(accountId)
            .audio()
            .getPlaylistsCustom(code)
            .map { items -> listEmptyIfNull(items.playlists) }
            .map { out ->
                val ret: MutableList<AudioPlaylist> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun getAudiosByArtist(
        accountId: Long,
        artist_id: String?,
        offset: Int,
        count: Int
    ): Single<List<Audio>> {
        return networker.vkDefault(accountId)
            .audio()
            .getAudiosByArtist(artist_id, offset, count)
            .map {
                listEmptyIfNull(
                    it.items
                )
            }
            .map { out ->
                val ret: MutableList<Audio> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun getById(accountId: Long, audios: List<Audio>): Single<List<Audio>> {
        return networker.vkDefault(accountId)
            .audio()
            .getById(audios)
            .map { out ->
                val ret: MutableList<Audio> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun getByIdOld(
        accountId: Long,
        audios: List<Audio>,
        old: Boolean
    ): Single<List<Audio>> {
        return if (!old) {
            getById(accountId, audios)
        } else networker.vkDefault(accountId)
            .audio()
            .getByIdOld(audios)
            .map { out ->
                val ret: MutableList<Audio> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun getLyrics(accountId: Long, lyrics_id: Int): Single<String> {
        return networker.vkDefault(accountId)
            .audio().getLyrics(lyrics_id).map { checkNotNull(it.text) }
    }

    override fun getArtistById(accountId: Long, artist_id: String): Single<ArtistInfo> {
        return networker.vkDefault(accountId)
            .audio().getArtistById(artist_id).map { it }
    }

    override fun getPopular(
        accountId: Long,
        foreign: Int,
        genre: Int,
        count: Int
    ): Single<List<Audio>> {
        return networker.vkDefault(accountId)
            .audio()
            .getPopular(foreign, genre, count)
            .map { out ->
                val ret: MutableList<Audio> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun getRecommendations(
        accountId: Long,
        audioOwnerId: Long,
        count: Int
    ): Single<List<Audio>> {
        return networker.vkDefault(accountId)
            .audio()
            .getRecommendations(audioOwnerId, count)
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
            .map { out ->
                val ret: MutableList<Audio> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun getRecommendationsByAudio(
        accountId: Long,
        audio: String?,
        count: Int
    ): Single<List<Audio>> {
        return networker.vkDefault(accountId)
            .audio()
            .getRecommendationsByAudio(audio, count)
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
            .map { out ->
                val ret: MutableList<Audio> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun getPlaylists(
        accountId: Long,
        owner_id: Long,
        offset: Int,
        count: Int
    ): Single<List<AudioPlaylist>> {
        return networker.vkDefault(accountId)
            .audio()
            .getPlaylists(owner_id, offset, count)
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
            .map { out ->
                val ret: MutableList<AudioPlaylist> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun createPlaylist(
        accountId: Long,
        ownerId: Long,
        title: String?,
        description: String?
    ): Single<AudioPlaylist> {
        return networker.vkDefault(accountId)
            .audio()
            .createPlaylist(ownerId, title, description)
            .map { out -> transform(out) }
    }

    override fun editPlaylist(
        accountId: Long,
        ownerId: Long,
        playlist_id: Int,
        title: String?,
        description: String?
    ): Single<Int> {
        return networker.vkDefault(accountId)
            .audio()
            .editPlaylist(ownerId, playlist_id, title, description)
            .map { it }
    }

    override fun removeFromPlaylist(
        accountId: Long,
        ownerId: Long,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Single<Int> {
        return networker.vkDefault(accountId)
            .audio()
            .removeFromPlaylist(ownerId, playlist_id, audio_ids)
    }

    override fun addToPlaylist(
        accountId: Long,
        ownerId: Long,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Single<List<AddToPlaylistResponse>> {
        return networker.vkDefault(accountId)
            .audio()
            .addToPlaylist(ownerId, playlist_id, audio_ids)
            .map { it }
    }

    override fun followPlaylist(
        accountId: Long,
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Single<AudioPlaylist> {
        return networker.vkDefault(accountId)
            .audio()
            .followPlaylist(playlist_id, ownerId, accessKey)
            .map { obj -> transform(obj) }
    }

    override fun clonePlaylist(
        accountId: Long,
        playlist_id: Int,
        ownerId: Long
    ): Single<AudioPlaylist> {
        return networker.vkDefault(accountId)
            .audio()
            .clonePlaylist(playlist_id, ownerId)
            .map { transform(it) }
    }

    override fun getPlaylistById(
        accountId: Long,
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Single<AudioPlaylist> {
        return networker.vkDefault(accountId)
            .audio()
            .getPlaylistById(playlist_id, ownerId, accessKey)
            .map { obj -> transform(obj) }
    }

    override fun reorder(
        accountId: Long,
        ownerId: Long,
        audio_id: Int,
        before: Int?,
        after: Int?
    ): Single<Int> {
        return networker.vkDefault(accountId)
            .audio()
            .reorder(ownerId, audio_id, before, after)
            .map { it }
    }

    @SuppressLint("DefaultLocale")
    override fun trackEvents(accountId: Long, audio: Audio): Completable {
        val events = String.format(
            "[{\"e\":\"audio_play\",\"audio_id\":\"%s\",\"source\":\"%s\",\"uuid\":%s,\"duration\":%d,\"start_time\":%d}]",
            audio.ownerId.toString() + "_" + audio.id,
            "my",
            System.nanoTime(),
            audio.duration,
            System.currentTimeMillis() / 1000
        )
        return networker.vkDefault(accountId)
            .audio()
            .trackEvents(events)
            .ignoreElement()
    }

    override fun deletePlaylist(accountId: Long, playlist_id: Int, ownerId: Long): Single<Int> {
        return networker.vkDefault(accountId)
            .audio()
            .deletePlaylist(playlist_id, ownerId)
    }

    override fun search(
        accountId: Long,
        criteria: AudioSearchCriteria,
        offset: Int,
        count: Int
    ): Single<List<Audio>> {
        val isMyAudio = criteria.extractBoleanValueFromOption(AudioSearchCriteria.KEY_SEARCH_ADDED)
        val isbyArtist =
            criteria.extractBoleanValueFromOption(AudioSearchCriteria.KEY_SEARCH_BY_ARTIST)
        val isautocmp =
            criteria.extractBoleanValueFromOption(AudioSearchCriteria.KEY_SEARCH_AUTOCOMPLETE)
        val islyrics =
            criteria.extractBoleanValueFromOption(AudioSearchCriteria.KEY_SEARCH_WITH_LYRICS)
        val sortOption = criteria.findOptionByKey<SpinnerOption>(AudioSearchCriteria.KEY_SORT)
        val sort =
            sortOption?.value?.id
        return networker.vkDefault(accountId)
            .audio()
            .search(criteria.query, isautocmp, islyrics, isbyArtist, sort, isMyAudio, offset, count)
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
            .map { out ->
                val ret: MutableList<Audio> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun searchArtists(
        accountId: Long,
        criteria: ArtistSearchCriteria,
        offset: Int,
        count: Int
    ): Single<List<VKApiArtist>> {
        return networker.vkDefault(accountId)
            .audio()
            .searchArtists(criteria.query, offset, count)
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
    }

    override fun searchPlaylists(
        accountId: Long,
        criteria: AudioPlaylistSearchCriteria,
        offset: Int,
        count: Int
    ): Single<List<AudioPlaylist>> {
        return networker.vkDefault(accountId)
            .audio()
            .searchPlaylists(criteria.query, offset, count)
            .map { items ->
                listEmptyIfNull(
                    items.items
                )
            }
            .map { out ->
                val ret: MutableList<AudioPlaylist> = ArrayList()
                for (i in out.indices) ret.add(transform(out[i]))
                ret
            }
    }

    override fun getCatalogV2Sections(
        accountId: Long,
        owner_id: Long,
        artist_id: String?,
        url: String?,
        query: String?,
        context: String?
    ): Single<CatalogV2List> {
        return networker.vkDefault(accountId)
            .audio()
            .getCatalogV2Sections(owner_id, artist_id, url, query, context)
            .map { CatalogV2List(it) }
    }

    override fun getCatalogV2Section(
        accountId: Long, section_id: String, start_from: String?
    ): Single<CatalogV2Section> {
        return networker.vkDefault(accountId)
            .audio()
            .getCatalogV2Section(section_id, start_from)
            .map { CatalogV2Section(it) }
    }

    override fun getCatalogV2BlockItems(
        accountId: Long, block_id: String, start_from: String?
    ): Single<CatalogV2Block> {
        return networker.vkDefault(accountId)
            .audio()
            .getCatalogV2BlockItems(block_id, start_from)
            .map { CatalogV2Block(it) }
    }
}
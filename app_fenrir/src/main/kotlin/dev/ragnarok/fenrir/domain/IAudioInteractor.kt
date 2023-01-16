package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.ArtistInfo
import dev.ragnarok.fenrir.api.model.VKApiArtist
import dev.ragnarok.fenrir.api.model.response.AddToPlaylistResponse
import dev.ragnarok.fenrir.fragment.search.criteria.ArtistSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.AudioPlaylistSearchCriteria
import dev.ragnarok.fenrir.fragment.search.criteria.AudioSearchCriteria
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.model.AudioPlaylist
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Block
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2List
import dev.ragnarok.fenrir.model.catalog_v2_audio.CatalogV2Section
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IAudioInteractor {
    fun add(accountId: Long, orig: Audio, groupId: Long?): Completable
    fun delete(accountId: Long, audioId: Int, ownerId: Long): Completable
    fun edit(
        accountId: Long,
        ownerId: Long,
        audioId: Int,
        artist: String?,
        title: String?,
        text: String?
    ): Completable

    fun restore(accountId: Long, audioId: Int, ownerId: Long): Completable
    fun sendBroadcast(
        accountId: Long,
        audioOwnerId: Long,
        audioId: Int,
        accessKey: String?,
        targetIds: Collection<Long>
    ): Completable

    operator fun get(
        accountId: Long,
        playlist_id: Int?,
        ownerId: Long,
        offset: Int,
        count: Int,
        accessKey: String?
    ): Single<List<Audio>>

    fun getPlaylistsCustom(accountId: Long, code: String?): Single<List<AudioPlaylist>>
    fun getAudiosByArtist(
        accountId: Long,
        artist_id: String?,
        offset: Int,
        count: Int
    ): Single<List<Audio>>

    fun getById(accountId: Long, audios: List<Audio>): Single<List<Audio>>
    fun getByIdOld(accountId: Long, audios: List<Audio>, old: Boolean): Single<List<Audio>>
    fun getLyrics(accountId: Long, lyrics_id: Int): Single<String>
    fun getPopular(accountId: Long, foreign: Int, genre: Int, count: Int): Single<List<Audio>>
    fun getRecommendations(accountId: Long, audioOwnerId: Long, count: Int): Single<List<Audio>>
    fun getRecommendationsByAudio(
        accountId: Long,
        audio: String?,
        count: Int
    ): Single<List<Audio>>

    fun search(
        accountId: Long,
        criteria: AudioSearchCriteria,
        offset: Int,
        count: Int
    ): Single<List<Audio>>

    fun searchArtists(
        accountId: Long,
        criteria: ArtistSearchCriteria,
        offset: Int,
        count: Int
    ): Single<List<VKApiArtist>>

    fun searchPlaylists(
        accountId: Long,
        criteria: AudioPlaylistSearchCriteria,
        offset: Int,
        count: Int
    ): Single<List<AudioPlaylist>>

    fun getPlaylists(
        accountId: Long,
        owner_id: Long,
        offset: Int,
        count: Int
    ): Single<List<AudioPlaylist>>

    fun createPlaylist(
        accountId: Long,
        ownerId: Long,
        title: String?,
        description: String?
    ): Single<AudioPlaylist>

    fun editPlaylist(
        accountId: Long,
        ownerId: Long,
        playlist_id: Int,
        title: String?,
        description: String?
    ): Single<Int>

    fun removeFromPlaylist(
        accountId: Long,
        ownerId: Long,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Single<Int>

    fun addToPlaylist(
        accountId: Long,
        ownerId: Long,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Single<List<AddToPlaylistResponse>>

    fun followPlaylist(
        accountId: Long,
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Single<AudioPlaylist>

    fun clonePlaylist(accountId: Long, playlist_id: Int, ownerId: Long): Single<AudioPlaylist>
    fun getPlaylistById(
        accountId: Long,
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Single<AudioPlaylist>

    fun deletePlaylist(accountId: Long, playlist_id: Int, ownerId: Long): Single<Int>
    fun reorder(
        accountId: Long,
        ownerId: Long,
        audio_id: Int,
        before: Int?,
        after: Int?
    ): Single<Int>

    fun trackEvents(accountId: Long, audio: Audio): Completable

    fun getCatalogV2Sections(
        accountId: Long,
        owner_id: Long,
        artist_id: String?,
        url: String?,
        query: String?,
        context: String?
    ): Single<CatalogV2List>

    fun getCatalogV2Section(
        accountId: Long, section_id: String, start_from: String?
    ): Single<CatalogV2Section>

    fun getCatalogV2BlockItems(
        accountId: Long, block_id: String, start_from: String?
    ): Single<CatalogV2Block>

    fun getArtistById(accountId: Long, artist_id: String): Single<ArtistInfo>
}
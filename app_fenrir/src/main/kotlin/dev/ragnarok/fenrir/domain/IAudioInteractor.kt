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
    fun add(accountId: Int, orig: Audio, groupId: Int?): Completable
    fun delete(accountId: Int, audioId: Int, ownerId: Int): Completable
    fun edit(
        accountId: Int,
        ownerId: Int,
        audioId: Int,
        artist: String?,
        title: String?,
        text: String?
    ): Completable

    fun restore(accountId: Int, audioId: Int, ownerId: Int): Completable
    fun sendBroadcast(
        accountId: Int,
        audioOwnerId: Int,
        audioId: Int,
        accessKey: String?,
        targetIds: Collection<Int>
    ): Completable

    operator fun get(
        accountId: Int,
        playlist_id: Int?,
        ownerId: Int,
        offset: Int,
        count: Int,
        accessKey: String?
    ): Single<List<Audio>>

    fun getPlaylistsCustom(accountId: Int, code: String?): Single<List<AudioPlaylist>>
    fun getAudiosByArtist(
        accountId: Int,
        artist_id: String?,
        offset: Int,
        count: Int
    ): Single<List<Audio>>

    fun getById(accountId: Int, audios: List<Audio>): Single<List<Audio>>
    fun getByIdOld(accountId: Int, audios: List<Audio>, old: Boolean): Single<List<Audio>>
    fun getLyrics(accountId: Int, lyrics_id: Int): Single<String>
    fun getPopular(accountId: Int, foreign: Int, genre: Int, count: Int): Single<List<Audio>>
    fun getRecommendations(accountId: Int, audioOwnerId: Int, count: Int): Single<List<Audio>>
    fun getRecommendationsByAudio(
        accountId: Int,
        audio: String?,
        count: Int
    ): Single<List<Audio>>

    fun search(
        accountId: Int,
        criteria: AudioSearchCriteria,
        offset: Int,
        count: Int
    ): Single<List<Audio>>

    fun searchArtists(
        accountId: Int,
        criteria: ArtistSearchCriteria,
        offset: Int,
        count: Int
    ): Single<List<VKApiArtist>>

    fun searchPlaylists(
        accountId: Int,
        criteria: AudioPlaylistSearchCriteria,
        offset: Int,
        count: Int
    ): Single<List<AudioPlaylist>>

    fun getPlaylists(
        accountId: Int,
        owner_id: Int,
        offset: Int,
        count: Int
    ): Single<List<AudioPlaylist>>

    fun createPlaylist(
        accountId: Int,
        ownerId: Int,
        title: String?,
        description: String?
    ): Single<AudioPlaylist>

    fun editPlaylist(
        accountId: Int,
        ownerId: Int,
        playlist_id: Int,
        title: String?,
        description: String?
    ): Single<Int>

    fun removeFromPlaylist(
        accountId: Int,
        ownerId: Int,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Single<Int>

    fun addToPlaylist(
        accountId: Int,
        ownerId: Int,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Single<List<AddToPlaylistResponse>>

    fun followPlaylist(
        accountId: Int,
        playlist_id: Int,
        ownerId: Int,
        accessKey: String?
    ): Single<AudioPlaylist>

    fun clonePlaylist(accountId: Int, playlist_id: Int, ownerId: Int): Single<AudioPlaylist>
    fun getPlaylistById(
        accountId: Int,
        playlist_id: Int,
        ownerId: Int,
        accessKey: String?
    ): Single<AudioPlaylist>

    fun deletePlaylist(accountId: Int, playlist_id: Int, ownerId: Int): Single<Int>
    fun reorder(
        accountId: Int,
        ownerId: Int,
        audio_id: Int,
        before: Int?,
        after: Int?
    ): Single<Int>

    fun trackEvents(accountId: Int, audio: Audio): Completable

    fun getCatalogV2Sections(
        accountId: Int,
        owner_id: Int,
        artist_id: String?,
        url: String?,
        query: String?,
        context: String?
    ): Single<CatalogV2List>

    fun getCatalogV2Section(
        accountId: Int, section_id: String, start_from: String?
    ): Single<CatalogV2Section>

    fun getCatalogV2BlockItems(
        accountId: Int, block_id: String, start_from: String?
    ): Single<CatalogV2Block>

    fun getArtistById(accountId: Int, artist_id: String): Single<ArtistInfo>
}
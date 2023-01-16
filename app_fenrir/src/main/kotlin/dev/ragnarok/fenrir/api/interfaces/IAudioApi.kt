package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2BlockResponse
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2ListResponse
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2SectionResponse
import dev.ragnarok.fenrir.api.model.response.AddToPlaylistResponse
import dev.ragnarok.fenrir.api.model.response.ServicePlaylistResponse
import dev.ragnarok.fenrir.api.model.server.VKApiAudioUploadServer
import dev.ragnarok.fenrir.model.Audio
import io.reactivex.rxjava3.core.Single

interface IAudioApi {
    @CheckResult
    fun setBroadcast(audio: AccessIdPair, targetIds: Collection<Long>): Single<List<Int>>

    @CheckResult
    fun search(
        query: String?, autoComplete: Boolean?, lyrics: Boolean?,
        performerOnly: Boolean?, sort: Int?, searchOwn: Boolean?,
        offset: Int?, count: Int?
    ): Single<Items<VKApiAudio>>

    @CheckResult
    fun searchArtists(query: String?, offset: Int?, count: Int?): Single<Items<VKApiArtist>>

    @CheckResult
    fun searchPlaylists(
        query: String?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiAudioPlaylist>>

    @CheckResult
    fun restore(audioId: Int, ownerId: Long?): Single<VKApiAudio>

    @CheckResult
    fun delete(audioId: Int, ownerId: Long): Single<Boolean>

    @CheckResult
    fun edit(
        ownerId: Long,
        audioId: Int,
        artist: String?,
        title: String?,
        text: String?
    ): Single<Int>

    @CheckResult
    fun add(audioId: Int, ownerId: Long, groupId: Long?, accessKey: String?): Single<Int>

    @CheckResult
    fun createPlaylist(
        ownerId: Long,
        title: String?,
        description: String?
    ): Single<VKApiAudioPlaylist>

    @CheckResult
    fun editPlaylist(
        ownerId: Long,
        playlist_id: Int,
        title: String?,
        description: String?
    ): Single<Int>

    @CheckResult
    fun removeFromPlaylist(
        ownerId: Long,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Single<Int>

    @CheckResult
    fun addToPlaylist(
        ownerId: Long,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Single<List<AddToPlaylistResponse>>

    @CheckResult
    fun reorder(ownerId: Long, audio_id: Int, before: Int?, after: Int?): Single<Int>

    @CheckResult
    fun trackEvents(events: String?): Single<Int>

    @CheckResult
    operator fun get(
        playlist_id: Int?, ownerId: Long?,
        offset: Int?, count: Int?, accessKey: String?
    ): Single<Items<VKApiAudio>>

    @CheckResult
    fun getAudiosByArtist(
        artist_id: String?,
        offset: Int?,
        count: Int?
    ): Single<Items<VKApiAudio>>

    @CheckResult
    fun getPopular(
        foreign: Int?,
        genre: Int?, count: Int?
    ): Single<List<VKApiAudio>>

    @CheckResult
    fun deletePlaylist(playlist_id: Int, ownerId: Long): Single<Int>

    @CheckResult
    fun followPlaylist(
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Single<VKApiAudioPlaylist>

    @CheckResult
    fun clonePlaylist(playlist_id: Int, ownerId: Long): Single<VKApiAudioPlaylist>

    @CheckResult
    fun getPlaylistById(
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Single<VKApiAudioPlaylist>

    @CheckResult
    fun getRecommendations(audioOwnerId: Long?, count: Int?): Single<Items<VKApiAudio>>

    @CheckResult
    fun getRecommendationsByAudio(audio: String?, count: Int?): Single<Items<VKApiAudio>>

    @CheckResult
    fun getById(audios: List<Audio>): Single<List<VKApiAudio>>

    @CheckResult
    fun getByIdOld(audios: List<Audio>): Single<List<VKApiAudio>>

    @CheckResult
    fun getLyrics(lyrics_id: Int): Single<VKApiLyrics>

    @CheckResult
    fun getPlaylists(owner_id: Long, offset: Int, count: Int): Single<Items<VKApiAudioPlaylist>>

    @CheckResult
    fun getPlaylistsCustom(code: String?): Single<ServicePlaylistResponse>

    @get:CheckResult
    val uploadServer: Single<VKApiAudioUploadServer>

    @CheckResult
    fun save(
        server: String?,
        audio: String?,
        hash: String?,
        artist: String?,
        title: String?
    ): Single<VKApiAudio>

    @CheckResult
    fun getCatalogV2Sections(
        owner_id: Long, artist_id: String?, url: String?, query: String?, context: String?
    ): Single<VKApiCatalogV2ListResponse>

    @CheckResult
    fun getCatalogV2Section(
        section_id: String,
        start_from: String?
    ): Single<VKApiCatalogV2SectionResponse>

    @CheckResult
    fun getCatalogV2BlockItems(
        block_id: String, start_from: String?
    ): Single<VKApiCatalogV2BlockResponse>

    @CheckResult
    fun getArtistById(
        artist_id: String
    ): Single<ArtistInfo>
}
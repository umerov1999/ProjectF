package dev.ragnarok.fenrir.api.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.api.model.AccessIdPair
import dev.ragnarok.fenrir.api.model.ArtistInfo
import dev.ragnarok.fenrir.api.model.Items
import dev.ragnarok.fenrir.api.model.VKApiArtist
import dev.ragnarok.fenrir.api.model.VKApiAudio
import dev.ragnarok.fenrir.api.model.VKApiAudioPlaylist
import dev.ragnarok.fenrir.api.model.VKApiLyrics
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2BlockResponse
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2ListResponse
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2SectionResponse
import dev.ragnarok.fenrir.api.model.response.AddToPlaylistResponse
import dev.ragnarok.fenrir.api.model.response.ServicePlaylistResponse
import dev.ragnarok.fenrir.api.model.server.VKApiAudioUploadServer
import dev.ragnarok.fenrir.model.Audio
import kotlinx.coroutines.flow.Flow

interface IAudioApi {
    @CheckResult
    fun setBroadcast(audio: AccessIdPair, targetIds: Collection<Long>): Flow<List<Int>>

    @CheckResult
    fun search(
        query: String?, autoComplete: Boolean?, lyrics: Boolean?,
        performerOnly: Boolean?, sort: Int?, searchOwn: Boolean?,
        offset: Int?, count: Int?
    ): Flow<Items<VKApiAudio>>

    @CheckResult
    fun searchArtists(query: String?, offset: Int?, count: Int?): Flow<Items<VKApiArtist>>

    @CheckResult
    fun searchPlaylists(
        query: String?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiAudioPlaylist>>

    @CheckResult
    fun restore(audioId: Int, ownerId: Long?): Flow<VKApiAudio>

    @CheckResult
    fun delete(audioId: Int, ownerId: Long): Flow<Boolean>

    @CheckResult
    fun edit(
        ownerId: Long,
        audioId: Int,
        artist: String?,
        title: String?
    ): Flow<Int>

    @CheckResult
    fun add(audioId: Int, ownerId: Long, groupId: Long?, accessKey: String?): Flow<Int>

    @CheckResult
    fun createPlaylist(
        ownerId: Long,
        title: String?,
        description: String?
    ): Flow<VKApiAudioPlaylist>

    @CheckResult
    fun editPlaylist(
        ownerId: Long,
        playlist_id: Int,
        title: String?,
        description: String?
    ): Flow<Int>

    @CheckResult
    fun removeFromPlaylist(
        ownerId: Long,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Flow<Int>

    @CheckResult
    fun addToPlaylist(
        ownerId: Long,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Flow<List<AddToPlaylistResponse>>

    @CheckResult
    fun reorder(ownerId: Long, audio_id: Int, before: Int?, after: Int?): Flow<Int>

    @CheckResult
    fun trackEvents(events: String?): Flow<Int>

    @CheckResult
    operator fun get(
        playlist_id: Int?, ownerId: Long?,
        offset: Int?, count: Int?, accessKey: String?
    ): Flow<Items<VKApiAudio>>

    @CheckResult
    fun getAudiosByArtist(
        artist_id: String?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiAudio>>

    @CheckResult
    fun getPopular(
        foreign: Int?,
        genre: Int?, count: Int?
    ): Flow<List<VKApiAudio>>

    @CheckResult
    fun deletePlaylist(playlist_id: Int, ownerId: Long): Flow<Int>

    @CheckResult
    fun followPlaylist(
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Flow<VKApiAudioPlaylist>

    @CheckResult
    fun clonePlaylist(playlist_id: Int, ownerId: Long): Flow<VKApiAudioPlaylist>

    @CheckResult
    fun getPlaylistById(
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Flow<VKApiAudioPlaylist>

    @CheckResult
    fun getRecommendations(audioOwnerId: Long?, count: Int?): Flow<Items<VKApiAudio>>

    @CheckResult
    fun getRecommendationsByAudio(audio: String?, count: Int?): Flow<Items<VKApiAudio>>

    @CheckResult
    fun getById(audios: List<Audio>): Flow<List<VKApiAudio>>

    @CheckResult
    fun getByIdOld(audios: List<Audio>): Flow<List<VKApiAudio>>

    @CheckResult
    fun getLyrics(audio: Audio): Flow<VKApiLyrics>

    @CheckResult
    fun getPlaylists(owner_id: Long, offset: Int, count: Int): Flow<Items<VKApiAudioPlaylist>>

    @CheckResult
    fun getPlaylistsCustom(code: String?): Flow<ServicePlaylistResponse>

    @get:CheckResult
    val uploadServer: Flow<VKApiAudioUploadServer>

    @CheckResult
    fun save(
        server: String?,
        audio: String?,
        hash: String?,
        artist: String?,
        title: String?
    ): Flow<VKApiAudio>

    @CheckResult
    fun getCatalogV2Sections(
        owner_id: Long, artist_id: String?, url: String?, query: String?, context: String?
    ): Flow<VKApiCatalogV2ListResponse>

    @CheckResult
    fun getCatalogV2Section(
        section_id: String,
        start_from: String?
    ): Flow<VKApiCatalogV2SectionResponse>

    @CheckResult
    fun getCatalogV2BlockItems(
        block_id: String, start_from: String?
    ): Flow<VKApiCatalogV2BlockResponse>

    @CheckResult
    fun getArtistById(
        artist_id: String
    ): Flow<ArtistInfo>
}
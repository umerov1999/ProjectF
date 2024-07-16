package dev.ragnarok.fenrir.api.impl

import dev.ragnarok.fenrir.Constants
import dev.ragnarok.fenrir.Includes
import dev.ragnarok.fenrir.api.IServiceProvider
import dev.ragnarok.fenrir.api.interfaces.IAudioApi
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
import dev.ragnarok.fenrir.api.services.IAudioService
import dev.ragnarok.fenrir.model.Audio
import dev.ragnarok.fenrir.nonNullNoEmpty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single

internal class AudioApi(accountId: Long, provider: IServiceProvider) :
    AbsApi(accountId, provider), IAudioApi {
    override fun setBroadcast(audio: AccessIdPair, targetIds: Collection<Long>): Flow<List<Int>> {
        val f = join(setOf(audio), ",") { AccessIdPair.format(it) }
        val s = join(targetIds, ",")
        return provideService(IAudioService())
            .flatMapConcat {
                it.setBroadcast(f, s)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun search(
        query: String?,
        autoComplete: Boolean?,
        lyrics: Boolean?,
        performerOnly: Boolean?,
        sort: Int?,
        searchOwn: Boolean?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiAudio>> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.search(
                    query,
                    integerFromBoolean(autoComplete),
                    integerFromBoolean(lyrics),
                    integerFromBoolean(performerOnly),
                    sort,
                    integerFromBoolean(searchOwn),
                    offset,
                    count
                )
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchArtists(
        query: String?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiArtist>> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.searchArtists(query, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun searchPlaylists(
        query: String?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiAudioPlaylist>> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.searchPlaylists(query, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun restore(audioId: Int, ownerId: Long?): Flow<VKApiAudio> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.restore(audioId, ownerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun delete(audioId: Int, ownerId: Long): Flow<Boolean> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.delete(audioId, ownerId)
                    .map(extractResponseWithErrorHandling())
                    .map { s ->
                        if (s == 1) {
                            Includes.stores.tempStore().deleteAudio(accountId, audioId, ownerId)
                                .single()
                        } else {
                            false
                        }
                    }
            }
    }

    override fun edit(
        ownerId: Long,
        audioId: Int,
        artist: String?,
        title: String?
    ): Flow<Int> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.edit(ownerId, audioId, artist, title)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun add(audioId: Int, ownerId: Long, groupId: Long?, accessKey: String?): Flow<Int> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.add(audioId, ownerId, groupId, accessKey)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun createPlaylist(
        ownerId: Long,
        title: String?,
        description: String?
    ): Flow<VKApiAudioPlaylist> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.createPlaylist(ownerId, title, description)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun editPlaylist(
        ownerId: Long,
        playlist_id: Int,
        title: String?,
        description: String?
    ): Flow<Int> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.editPlaylist(ownerId, playlist_id, title, description)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun removeFromPlaylist(
        ownerId: Long,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Flow<Int> {
        return provideService(IAudioService())
            .flatMapConcat { s ->
                s.removeFromPlaylist(
                    ownerId,
                    playlist_id,
                    join(audio_ids, ",") { AccessIdPair.format(it) })
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun addToPlaylist(
        ownerId: Long,
        playlist_id: Int,
        audio_ids: Collection<AccessIdPair>
    ): Flow<List<AddToPlaylistResponse>> {
        return provideService(IAudioService())
            .flatMapConcat { s ->
                s.addToPlaylist(
                    ownerId,
                    playlist_id,
                    join(audio_ids, ",") { AccessIdPair.format(it) })
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun reorder(ownerId: Long, audio_id: Int, before: Int?, after: Int?): Flow<Int> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.reorder(ownerId, audio_id, before, after)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun trackEvents(events: String?): Flow<Int> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.trackEvents(events)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getCatalogV2Sections(
        owner_id: Long, artist_id: String?, url: String?, query: String?, context: String?
    ): Flow<VKApiCatalogV2ListResponse> {
        return provideService(IAudioService())
            .flatMapConcat { service ->
                (if (artist_id.nonNullNoEmpty()) service.getCatalogV2Artist(
                    artist_id,
                    0
                ) else if (query.nonNullNoEmpty()) service.getCatalogV2AudioSearch(
                    query,
                    context,
                    0
                ) else service.getCatalogV2Sections(
                    owner_id, 0, url
                ))
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getCatalogV2BlockItems(
        block_id: String, start_from: String?
    ): Flow<VKApiCatalogV2BlockResponse> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.getCatalogV2BlockItems(block_id, start_from)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getCatalogV2Section(
        section_id: String,
        start_from: String?
    ): Flow<VKApiCatalogV2SectionResponse> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.getCatalogV2Section(section_id, start_from)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun deletePlaylist(playlist_id: Int, ownerId: Long): Flow<Int> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.deletePlaylist(playlist_id, ownerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun followPlaylist(
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Flow<VKApiAudioPlaylist> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.followPlaylist(playlist_id, ownerId, accessKey)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun clonePlaylist(playlist_id: Int, ownerId: Long): Flow<VKApiAudioPlaylist> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.clonePlaylist(playlist_id, ownerId)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getPlaylistById(
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Flow<VKApiAudioPlaylist> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.getPlaylistById(playlist_id, ownerId, accessKey)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun get(
        playlist_id: Int?,
        ownerId: Long?,
        offset: Int?,
        count: Int?,
        accessKey: String?
    ): Flow<Items<VKApiAudio>> {
        return provideService(IAudioService())
            .flatMapConcat {
                it[playlist_id, ownerId, offset, count, accessKey].map(
                    extractResponseWithErrorHandling()
                )
            }
    }

    override fun getAudiosByArtist(
        artist_id: String?,
        offset: Int?,
        count: Int?
    ): Flow<Items<VKApiAudio>> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.getAudiosByArtist(artist_id, offset, count).map(
                    extractResponseWithErrorHandling()
                )
            }
    }

    override fun getPopular(
        foreign: Int?,
        genre: Int?, count: Int?
    ): Flow<List<VKApiAudio>> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.getPopular(foreign, genre, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getRecommendations(audioOwnerId: Long?, count: Int?): Flow<Items<VKApiAudio>> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.getRecommendations(audioOwnerId, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getRecommendationsByAudio(audio: String?, count: Int?): Flow<Items<VKApiAudio>> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.getRecommendationsByAudio(audio, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getPlaylists(
        owner_id: Long,
        offset: Int,
        count: Int
    ): Flow<Items<VKApiAudioPlaylist>> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.getPlaylists(owner_id, offset, count)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getPlaylistsCustom(code: String?): Flow<ServicePlaylistResponse> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.getPlaylistsCustom(code)
            }
    }

    override fun getById(audios: List<Audio>): Flow<List<VKApiAudio>> {
        val ids = ArrayList<AccessIdPair>(audios.size)
        for (i in audios) {
            ids.add(AccessIdPair(i.id, i.ownerId, i.accessKey))
        }
        val audio_string = join(ids, ",") { AccessIdPair.format(it) }
        return provideService(IAudioService())
            .flatMapConcat {
                it.getById(audio_string)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getByIdOld(audios: List<Audio>): Flow<List<VKApiAudio>> {
        val ids = ArrayList<AccessIdPair>(audios.size)
        for (i in audios) {
            ids.add(AccessIdPair(i.id, i.ownerId, i.accessKey))
        }
        val audio_string = join(ids, ",") { AccessIdPair.format(it) }
        return provideService(IAudioService())
            .flatMapConcat {
                it.getByIdVersioned(audio_string, Constants.OLD_API_FOR_AUDIO_VERSION)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getLyrics(audio: Audio): Flow<VKApiLyrics> {
        return provideService(IAudioService())
            .flatMapConcat { s ->
                s.getLyrics(
                    join(
                        listOf(AccessIdPair(audio.id, audio.ownerId, audio.accessKey)),
                        ","
                    ) { AccessIdPair.format(it) })
                    .map(extractResponseWithErrorHandling())
            }
    }

    override val uploadServer: Flow<VKApiAudioUploadServer>
        get() = provideService(IAudioService())
            .flatMapConcat {
                it.uploadServer
                    .map(extractResponseWithErrorHandling())
            }

    override fun save(
        server: String?,
        audio: String?,
        hash: String?,
        artist: String?,
        title: String?
    ): Flow<VKApiAudio> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.save(server, audio, hash, artist, title)
                    .map(extractResponseWithErrorHandling())
            }
    }

    override fun getArtistById(
        artist_id: String
    ): Flow<ArtistInfo> {
        return provideService(IAudioService())
            .flatMapConcat {
                it.getArtistById(artist_id)
                    .map(extractResponseWithErrorHandling())
            }
    }
}
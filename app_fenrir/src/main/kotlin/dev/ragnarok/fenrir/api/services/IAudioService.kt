package dev.ragnarok.fenrir.api.services

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
import dev.ragnarok.fenrir.api.model.response.BaseResponse
import dev.ragnarok.fenrir.api.model.response.ServicePlaylistResponse
import dev.ragnarok.fenrir.api.model.server.VKApiAudioUploadServer
import dev.ragnarok.fenrir.api.rest.IServiceRest
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.builtins.serializer

class IAudioService : IServiceRest() {
    fun setBroadcast(
        audio: String?,
        targetIds: String?
    ): Flow<BaseResponse<List<Int>>> {
        return rest.request(
            "audio.setBroadcast", form("audio" to audio, "target_ids" to targetIds), baseList(
                Int.serializer()
            )
        )
    }

    //https://vk.com/dev/audio.search
    fun search(
        query: String?,
        autoComplete: Int?,
        lyrics: Int?,
        performerOnly: Int?,
        sort: Int?,
        searchOwn: Int?,
        offset: Int?,
        count: Int?
    ): Flow<BaseResponse<Items<VKApiAudio>>> {
        return rest.request(
            "audio.search",
            form(
                "q" to query,
                "auto_complete" to autoComplete,
                "lyrics" to lyrics,
                "performer_only" to performerOnly,
                "sort" to sort,
                "search_own" to searchOwn,
                "offset" to offset,
                "count" to count
            ), items(VKApiAudio.serializer())
        )
    }

    //https://vk.com/dev/audio.searchArtists
    fun searchArtists(
        query: String?,
        offset: Int?,
        count: Int?
    ): Flow<BaseResponse<Items<VKApiArtist>>> {
        return rest.request(
            "audio.searchArtists",
            form("q" to query, "offset" to offset, "count" to count),
            items(VKApiArtist.serializer())
        )
    }

    //https://vk.com/dev/audio.searchPlaylists
    fun searchPlaylists(
        query: String?,
        offset: Int?,
        count: Int?
    ): Flow<BaseResponse<Items<VKApiAudioPlaylist>>> {
        return rest.request(
            "audio.searchPlaylists",
            form("q" to query, "offset" to offset, "count" to count),
            items(VKApiAudioPlaylist.serializer())
        )
    }

    //https://vk.com/dev/audio.restore
    fun restore(
        audioId: Int,
        ownerId: Long?
    ): Flow<BaseResponse<VKApiAudio>> {
        return rest.request(
            "audio.restore",
            form("audio_id" to audioId, "owner_id" to ownerId),
            base(VKApiAudio.serializer())
        )
    }

    //https://vk.com/dev/audio.delete
    fun delete(
        audioId: Int,
        ownerId: Long
    ): Flow<BaseResponse<Int>> {
        return rest.request(
            "audio.delete",
            form("audio_id" to audioId, "owner_id" to ownerId),
            baseInt
        )
    }

    //https://vk.com/dev/audio.add
    fun add(
        audioId: Int,
        ownerId: Long,
        groupId: Long?,
        accessKey: String?
    ): Flow<BaseResponse<Int>> {
        return rest.request(
            "audio.add",
            form(
                "audio_id" to audioId,
                "owner_id" to ownerId,
                "group_id" to groupId,
                "access_key" to accessKey
            ),
            baseInt
        )
    }

    /**
     * Returns a list of audio files of a user or community.
     *
     * @param ownerId ID of the user or community that owns the audio file.
     * Use a negative value to designate a community ID.
     * Current user id is used by default
     * @param offset  Offset needed to return a specific subset of audio files.
     * @param count   Number of audio files to return.
     * @return Returns the total results number in count field and an array of objects describing audio in items field.
     */
    //https://vk.com/dev/audio.get
    operator fun get(
        playlist_id: Int?,
        ownerId: Long?,
        offset: Int?,
        count: Int?,
        accessKey: String?
    ): Flow<BaseResponse<Items<VKApiAudio>>> {
        return rest.request(
            "audio.get",
            form(
                "playlist_id" to playlist_id,
                "owner_id" to ownerId,
                "offset" to offset,
                "count" to count,
                "access_key" to accessKey
            ), items(VKApiAudio.serializer())
        )
    }

    //https://vk.com/dev/audio.getAudiosByArtist
    fun getAudiosByArtist(
        artist_id: String?,
        offset: Int?,
        count: Int?
    ): Flow<BaseResponse<Items<VKApiAudio>>> {
        return rest.request(
            "audio.getAudiosByArtist",
            form("artist_id" to artist_id, "offset" to offset, "count" to count),
            items(VKApiAudio.serializer())
        )
    }

    fun getPopular(
        only_eng: Int?,
        genre: Int?,
        count: Int?
    ): Flow<BaseResponse<List<VKApiAudio>>> {
        return rest.request(
            "audio.getPopular",
            form("only_eng" to only_eng, "genre_id" to genre, "count" to count),
            baseList(VKApiAudio.serializer())
        )
    }

    fun getRecommendations(
        user_id: Long?,
        count: Int?
    ): Flow<BaseResponse<Items<VKApiAudio>>> {
        return rest.request(
            "audio.getRecommendations",
            form("user_id" to user_id, "count" to count),
            items(VKApiAudio.serializer())
        )
    }

    fun getRecommendationsByAudio(
        audio: String?,
        count: Int?
    ): Flow<BaseResponse<Items<VKApiAudio>>> {
        return rest.request(
            "audio.getRecommendations",
            form("target_audio" to audio, "count" to count),
            items(VKApiAudio.serializer())
        )
    }

    fun getById(audios: String?): Flow<BaseResponse<List<VKApiAudio>>> {
        return rest.request(
            "audio.getById",
            form("audios" to audios),
            baseList(VKApiAudio.serializer())
        )
    }

    fun getByIdVersioned(
        audios: String?,
        version: String?
    ): Flow<BaseResponse<List<VKApiAudio>>> {
        return rest.request(
            "audio.getById",
            form("audios" to audios, "v" to version),
            baseList(VKApiAudio.serializer())
        )
    }

    fun getLyrics(audio: String?): Flow<BaseResponse<VKApiLyrics>> {
        return rest.request(
            "audio.getLyrics",
            form("audio_id" to audio),
            base(VKApiLyrics.serializer())
        )
    }

    fun getPlaylists(
        owner_id: Long,
        offset: Int,
        count: Int
    ): Flow<BaseResponse<Items<VKApiAudioPlaylist>>> {
        return rest.request(
            "audio.getPlaylists",
            form("owner_id" to owner_id, "offset" to offset, "count" to count),
            items(VKApiAudioPlaylist.serializer())
        )
    }

    fun getPlaylistsCustom(code: String?): Flow<ServicePlaylistResponse> {
        return rest.request("execute", form("code" to code), ServicePlaylistResponse.serializer())
    }

    fun deletePlaylist(
        playlist_id: Int,
        ownerId: Long
    ): Flow<BaseResponse<Int>> {
        return rest.request(
            "audio.deletePlaylist",
            form("playlist_id" to playlist_id, "owner_id" to ownerId),
            baseInt
        )
    }

    fun followPlaylist(
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Flow<BaseResponse<VKApiAudioPlaylist>> {
        return rest.request(
            "audio.followPlaylist",
            form("playlist_id" to playlist_id, "owner_id" to ownerId, "access_key" to accessKey),
            base(VKApiAudioPlaylist.serializer())
        )
    }

    fun clonePlaylist(
        playlist_id: Int,
        ownerId: Long
    ): Flow<BaseResponse<VKApiAudioPlaylist>> {
        return rest.request(
            "audio.savePlaylistAsCopy",
            form("playlist_id" to playlist_id, "owner_id" to ownerId),
            base(VKApiAudioPlaylist.serializer())
        )
    }

    fun getPlaylistById(
        playlist_id: Int,
        ownerId: Long,
        accessKey: String?
    ): Flow<BaseResponse<VKApiAudioPlaylist>> {
        return rest.request(
            "audio.getPlaylistById",
            form("playlist_id" to playlist_id, "owner_id" to ownerId, "access_key" to accessKey),
            base(VKApiAudioPlaylist.serializer())
        )
    }

    val uploadServer: Flow<BaseResponse<VKApiAudioUploadServer>>
        get() = rest.request(
            "audio.getUploadServer",
            null,
            base(VKApiAudioUploadServer.serializer())
        )

    fun save(
        server: String?,
        audio: String?,
        hash: String?,
        artist: String?,
        title: String?
    ): Flow<BaseResponse<VKApiAudio>> {
        return rest.request(
            "audio.save",
            form(
                "server" to server,
                "audio" to audio,
                "hash" to hash,
                "artist" to artist,
                "title" to title
            ),
            base(VKApiAudio.serializer())
        )
    }

    fun edit(
        ownerId: Long,
        audioId: Int,
        artist: String?,
        title: String?
    ): Flow<BaseResponse<Int>> {
        return rest.request(
            "audio.edit",
            form(
                "owner_id" to ownerId,
                "audio_id" to audioId,
                "artist" to artist,
                "title" to title,
            ),
            baseInt
        )
    }

    fun createPlaylist(
        ownerId: Long,
        title: String?,
        description: String?
    ): Flow<BaseResponse<VKApiAudioPlaylist>> {
        return rest.request(
            "audio.createPlaylist",
            form("owner_id" to ownerId, "title" to title, "description" to description),
            base(VKApiAudioPlaylist.serializer())
        )
    }

    fun editPlaylist(
        ownerId: Long,
        playlist_id: Int,
        title: String?,
        description: String?
    ): Flow<BaseResponse<Int>> {
        return rest.request(
            "audio.editPlaylist",
            form(
                "owner_id" to ownerId,
                "playlist_id" to playlist_id,
                "title" to title,
                "description" to description
            ), baseInt
        )
    }

    fun removeFromPlaylist(
        ownerId: Long,
        playlist_id: Int,
        audio_ids: String?
    ): Flow<BaseResponse<Int>> {
        return rest.request(
            "audio.removeFromPlaylist",
            form("owner_id" to ownerId, "playlist_id" to playlist_id, "audio_ids" to audio_ids),
            baseInt
        )
    }

    fun addToPlaylist(
        ownerId: Long,
        playlist_id: Int,
        audio_ids: String?
    ): Flow<BaseResponse<List<AddToPlaylistResponse>>> {
        return rest.request(
            "audio.addToPlaylist",
            form("owner_id" to ownerId, "playlist_id" to playlist_id, "audio_ids" to audio_ids),
            baseList(AddToPlaylistResponse.serializer())
        )
    }

    fun reorder(
        ownerId: Long,
        audio_id: Int,
        before: Int?,
        after: Int?
    ): Flow<BaseResponse<Int>> {
        return rest.request(
            "audio.reorder",
            form(
                "owner_id" to ownerId,
                "audio_id" to audio_id,
                "before" to before,
                "after" to after
            ),
            baseInt
        )
    }

    fun trackEvents(events: String?): Flow<BaseResponse<Int>> {
        return rest.request("stats.trackEvents", form("events" to events), baseInt)
    }

    fun getCatalogV2Sections(
        owner_id: Long,
        need_blocks: Int,
        url: String?
    ): Flow<BaseResponse<VKApiCatalogV2ListResponse>> {
        return rest.request(
            "catalog.getAudio",
            form("owner_id" to owner_id, "need_blocks" to need_blocks, "url" to url),
            base(VKApiCatalogV2ListResponse.serializer())
        )
    }

    fun getCatalogV2Artist(
        artist_id: String,
        need_blocks: Int
    ): Flow<BaseResponse<VKApiCatalogV2ListResponse>> {
        return rest.request(
            "catalog.getAudioArtist",
            form("artist_id" to artist_id, "need_blocks" to need_blocks),
            base(VKApiCatalogV2ListResponse.serializer())
        )
    }

    fun getCatalogV2Section(
        section_id: String,
        start_from: String?
    ): Flow<BaseResponse<VKApiCatalogV2SectionResponse>> {
        return rest.request(
            "catalog.getSection",
            form("section_id" to section_id, "start_from" to start_from),
            base(VKApiCatalogV2SectionResponse.serializer())
        )
    }

    fun getCatalogV2BlockItems(
        block_id: String,
        start_from: String?
    ): Flow<BaseResponse<VKApiCatalogV2BlockResponse>> {
        return rest.request(
            "catalog.getBlockItems",
            form("block_id" to block_id, "start_from" to start_from),
            base(VKApiCatalogV2BlockResponse.serializer())
        )
    }

    fun getCatalogV2AudioSearch(
        query: String?,
        context: String?,
        need_blocks: Int
    ): Flow<BaseResponse<VKApiCatalogV2ListResponse>> {
        return rest.request(
            "catalog.getAudioSearch",
            form("query" to query, "context" to context, "need_blocks" to need_blocks),
            base(VKApiCatalogV2ListResponse.serializer())
        )
    }

    fun getArtistById(
        artist_id: String
    ): Flow<BaseResponse<ArtistInfo>> {
        return rest.request(
            "audio.getArtistById",
            form("artist_id" to artist_id),
            base(ArtistInfo.serializer())
        )
    }
}
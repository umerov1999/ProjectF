package dev.ragnarok.fenrir.api.services

import dev.ragnarok.fenrir.api.model.*
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2BlockResponse
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2ListResponse
import dev.ragnarok.fenrir.api.model.catalog_v2_audio.VKApiCatalogV2SectionResponse
import dev.ragnarok.fenrir.api.model.response.*
import dev.ragnarok.fenrir.api.model.server.VKApiAudioUploadServer
import io.reactivex.rxjava3.core.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface IAudioService {
    @FormUrlEncoded
    @POST("audio.setBroadcast")
    fun setBroadcast(
        @Field("audio") audio: String?,
        @Field("target_ids") targetIds: String?
    ): Single<BaseResponse<List<Int>>>

    //https://vk.com/dev/audio.search
    @FormUrlEncoded
    @POST("audio.search")
    fun search(
        @Field("q") query: String?,
        @Field("auto_complete") autoComplete: Int?,
        @Field("lyrics") lyrics: Int?,
        @Field("performer_only") performerOnly: Int?,
        @Field("sort") sort: Int?,
        @Field("search_own") searchOwn: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiAudio>>>

    //https://vk.com/dev/audio.searchArtists
    @FormUrlEncoded
    @POST("audio.searchArtists")
    fun searchArtists(
        @Field("q") query: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiArtist>>>

    //https://vk.com/dev/audio.searchPlaylists
    @FormUrlEncoded
    @POST("audio.searchPlaylists")
    fun searchPlaylists(
        @Field("q") query: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiAudioPlaylist>>>

    //https://vk.com/dev/audio.restore
    @FormUrlEncoded
    @POST("audio.restore")
    fun restore(
        @Field("audio_id") audioId: Int,
        @Field("owner_id") ownerId: Int?
    ): Single<BaseResponse<VKApiAudio>>

    //https://vk.com/dev/audio.delete
    @FormUrlEncoded
    @POST("audio.delete")
    fun delete(
        @Field("audio_id") audioId: Int,
        @Field("owner_id") ownerId: Int
    ): Single<BaseResponse<Int>>

    //https://vk.com/dev/audio.add
    @FormUrlEncoded
    @POST("audio.add")
    fun add(
        @Field("audio_id") audioId: Int,
        @Field("owner_id") ownerId: Int,
        @Field("group_id") groupId: Int?,
        @Field("access_key") accessKey: String?
    ): Single<BaseResponse<Int>>

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
    @FormUrlEncoded
    @POST("audio.get")
    operator fun get(
        @Field("playlist_id") playlist_id: Int?,
        @Field("owner_id") ownerId: Int?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?,
        @Field("access_key") accessKey: String?
    ): Single<BaseResponse<Items<VKApiAudio>>>

    //https://vk.com/dev/audio.getAudiosByArtist
    @FormUrlEncoded
    @POST("audio.getAudiosByArtist")
    fun getAudiosByArtist(
        @Field("artist_id") artist_id: String?,
        @Field("offset") offset: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiAudio>>>

    @FormUrlEncoded
    @POST("audio.getPopular")
    fun getPopular(
        @Field("only_eng") foreign: Int?,
        @Field("genre_id") genre: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<List<VKApiAudio>>>

    @FormUrlEncoded
    @POST("audio.getRecommendations")
    fun getRecommendations(
        @Field("user_id") user_id: Int?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiAudio>>>

    @FormUrlEncoded
    @POST("audio.getRecommendations")
    fun getRecommendationsByAudio(
        @Field("target_audio") audio: String?,
        @Field("count") count: Int?
    ): Single<BaseResponse<Items<VKApiAudio>>>

    @FormUrlEncoded
    @POST("audio.getById")
    fun getById(@Field("audios") audios: String?): Single<BaseResponse<List<VKApiAudio>>>

    @FormUrlEncoded
    @POST("audio.getById")
    fun getByIdVersioned(
        @Field("audios") audios: String?,
        @Field("v") version: String?
    ): Single<BaseResponse<List<VKApiAudio>>>

    @FormUrlEncoded
    @POST("audio.getLyrics")
    fun getLyrics(@Field("lyrics_id") lyrics_id: Int): Single<BaseResponse<VKApiLyrics>>

    @FormUrlEncoded
    @POST("audio.getPlaylists")
    fun getPlaylists(
        @Field("owner_id") owner_id: Int,
        @Field("offset") offset: Int,
        @Field("count") count: Int
    ): Single<BaseResponse<Items<VKApiAudioPlaylist>>>

    @FormUrlEncoded
    @POST("execute")
    fun getPlaylistsCustom(@Field("code") code: String?): Single<ServicePlaylistResponse>

    @FormUrlEncoded
    @POST("audio.deletePlaylist")
    fun deletePlaylist(
        @Field("playlist_id") playlist_id: Int,
        @Field("owner_id") ownerId: Int
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("audio.followPlaylist")
    fun followPlaylist(
        @Field("playlist_id") playlist_id: Int,
        @Field("owner_id") ownerId: Int,
        @Field("access_key") accessKey: String?
    ): Single<BaseResponse<VKApiAudioPlaylist>>

    @FormUrlEncoded
    @POST("audio.savePlaylistAsCopy")
    fun clonePlaylist(
        @Field("playlist_id") playlist_id: Int,
        @Field("owner_id") ownerId: Int
    ): Single<BaseResponse<VKApiAudioPlaylist>>

    @FormUrlEncoded
    @POST("audio.getPlaylistById")
    fun getPlaylistById(
        @Field("playlist_id") playlist_id: Int,
        @Field("owner_id") ownerId: Int,
        @Field("access_key") accessKey: String?
    ): Single<BaseResponse<VKApiAudioPlaylist>>

    @get:POST("audio.getUploadServer")
    val uploadServer: Single<BaseResponse<VKApiAudioUploadServer>>

    @FormUrlEncoded
    @POST("audio.save")
    fun save(
        @Field("server") server: String?,
        @Field("audio") audio: String?,
        @Field("hash") hash: String?,
        @Field("artist") artist: String?,
        @Field("title") title: String?
    ): Single<BaseResponse<VKApiAudio>>

    @FormUrlEncoded
    @POST("audio.edit")
    fun edit(
        @Field("owner_id") ownerId: Int,
        @Field("audio_id") audioId: Int,
        @Field("artist") artist: String?,
        @Field("title") title: String?,
        @Field("text") text: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("audio.createPlaylist")
    fun createPlaylist(
        @Field("owner_id") ownerId: Int,
        @Field("title") title: String?,
        @Field("description") description: String?
    ): Single<BaseResponse<VKApiAudioPlaylist>>

    @FormUrlEncoded
    @POST("audio.editPlaylist")
    fun editPlaylist(
        @Field("owner_id") ownerId: Int,
        @Field("playlist_id") playlist_id: Int,
        @Field("title") title: String?,
        @Field("description") description: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("audio.removeFromPlaylist")
    fun removeFromPlaylist(
        @Field("owner_id") ownerId: Int,
        @Field("playlist_id") playlist_id: Int,
        @Field("audio_ids") audio_ids: String?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("audio.addToPlaylist")
    fun addToPlaylist(
        @Field("owner_id") ownerId: Int,
        @Field("playlist_id") playlist_id: Int,
        @Field("audio_ids") audio_ids: String?
    ): Single<BaseResponse<List<AddToPlaylistResponse>>>

    @FormUrlEncoded
    @POST("audio.reorder")
    fun reorder(
        @Field("owner_id") ownerId: Int,
        @Field("audio_id") audio_id: Int,
        @Field("before") before: Int?,
        @Field("after") after: Int?
    ): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("stats.trackEvents")
    fun trackEvents(@Field("events") events: String?): Single<BaseResponse<Int>>

    @FormUrlEncoded
    @POST("catalog.getAudio")
    fun getCatalogV2Sections(
        @Field("owner_id") owner_id: Int,
        @Field("need_blocks") need_blocks: Int,
        @Field("url") url: String?
    ): Single<BaseResponse<VKApiCatalogV2ListResponse>>

    @FormUrlEncoded
    @POST("catalog.getAudioArtist")
    fun getCatalogV2Artist(
        @Field("artist_id") artist_id: String,
        @Field("need_blocks") need_blocks: Int
    ): Single<BaseResponse<VKApiCatalogV2ListResponse>>

    @FormUrlEncoded
    @POST("catalog.getSection")
    fun getCatalogV2Section(
        @Field("section_id") section_id: String,
        @Field("start_from") start_from: String?
    ): Single<BaseResponse<VKApiCatalogV2SectionResponse>>

    @FormUrlEncoded
    @POST("catalog.getBlockItems")
    fun getCatalogV2BlockItems(
        @Field("block_id") block_id: String,
        @Field("start_from") start_from: String?
    ): Single<BaseResponse<VKApiCatalogV2BlockResponse>>

    @FormUrlEncoded
    @POST("catalog.getAudioSearch")
    fun getCatalogV2AudioSearch(
        @Field("query") query: String?,
        @Field("context") context: String?,
        @Field("need_blocks") need_blocks: Int
    ): Single<BaseResponse<VKApiCatalogV2ListResponse>>

    @FormUrlEncoded
    @POST("audio.getArtistById")
    fun getArtistById(
        @Field("artist_id") artist_id: String
    ): Single<BaseResponse<ArtistInfo>>
}
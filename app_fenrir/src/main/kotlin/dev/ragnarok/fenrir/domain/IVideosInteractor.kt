package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.criteria.VideoSearchCriteria
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.VideoAlbum
import dev.ragnarok.fenrir.util.Pair
import kotlinx.coroutines.flow.Flow

interface IVideosInteractor {
    operator fun get(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        count: Int,
        offset: Int
    ): Flow<List<Video>>

    fun getCachedVideos(accountId: Long, ownerId: Long, albumId: Int): Flow<List<Video>>
    fun getById(
        accountId: Long,
        ownerId: Long,
        videoId: Int,
        accessKey: String?,
        cache: Boolean
    ): Flow<Video>

    fun addToMy(
        accountId: Long,
        targetOwnerId: Long,
        videoOwnerId: Long,
        videoId: Int
    ): Flow<Boolean>

    fun likeOrDislike(
        accountId: Long,
        ownerId: Long,
        videoId: Int,
        accessKey: String?,
        like: Boolean
    ): Flow<Pair<Int, Boolean>>

    fun isLiked(accountId: Long, ownerId: Long, videoId: Int): Flow<Boolean>
    fun checkAndAddLike(
        accountId: Long,
        ownerId: Long,
        videoId: Int,
        accessKey: String?
    ): Flow<Int>

    fun getCachedAlbums(accountId: Long, ownerId: Long): Flow<List<VideoAlbum>>
    fun getActualAlbums(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Flow<List<VideoAlbum>>

    fun getAlbumsByVideo(
        accountId: Long,
        target_id: Long,
        owner_id: Long,
        video_id: Int
    ): Flow<List<VideoAlbum>>

    fun search(
        accountId: Long,
        criteria: VideoSearchCriteria,
        count: Int,
        offset: Int
    ): Flow<List<Video>>

    fun edit(
        accountId: Long,
        ownerId: Long,
        video_id: Int,
        name: String?,
        desc: String?
    ): Flow<Boolean>

    fun delete(accountId: Long, videoId: Int?, ownerId: Long?, targetId: Long?): Flow<Boolean>
}
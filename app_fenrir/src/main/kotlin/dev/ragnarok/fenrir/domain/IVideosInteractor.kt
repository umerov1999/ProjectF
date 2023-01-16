package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.criteria.VideoSearchCriteria
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.VideoAlbum
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IVideosInteractor {
    operator fun get(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        count: Int,
        offset: Int
    ): Single<List<Video>>

    fun getCachedVideos(accountId: Long, ownerId: Long, albumId: Int): Single<List<Video>>
    fun getById(
        accountId: Long,
        ownerId: Long,
        videoId: Int,
        accessKey: String?,
        cache: Boolean
    ): Single<Video>

    fun addToMy(accountId: Long, targetOwnerId: Long, videoOwnerId: Long, videoId: Int): Completable
    fun likeOrDislike(
        accountId: Long,
        ownerId: Long,
        videoId: Int,
        accessKey: String?,
        like: Boolean
    ): Single<Pair<Int, Boolean>>

    fun isLiked(accountId: Long, ownerId: Long, videoId: Int): Single<Boolean>
    fun checkAndAddLike(
        accountId: Long,
        ownerId: Long,
        videoId: Int,
        accessKey: String?
    ): Single<Int>

    fun getCachedAlbums(accountId: Long, ownerId: Long): Single<List<VideoAlbum>>
    fun getActualAlbums(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Single<List<VideoAlbum>>

    fun getAlbumsByVideo(
        accountId: Long,
        target_id: Long,
        owner_id: Long,
        video_id: Int
    ): Single<List<VideoAlbum>>

    fun search(
        accountId: Long,
        criteria: VideoSearchCriteria,
        count: Int,
        offset: Int
    ): Single<List<Video>>

    fun edit(
        accountId: Long,
        ownerId: Long,
        video_id: Int,
        name: String?,
        desc: String?
    ): Completable

    fun delete(accountId: Long, videoId: Int?, ownerId: Long?, targetId: Long?): Completable
}
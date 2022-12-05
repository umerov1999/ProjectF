package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.criteria.VideoSearchCriteria
import dev.ragnarok.fenrir.model.Video
import dev.ragnarok.fenrir.model.VideoAlbum
import dev.ragnarok.fenrir.util.Pair
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IVideosInteractor {
    operator fun get(
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        count: Int,
        offset: Int
    ): Single<List<Video>>

    fun getCachedVideos(accountId: Int, ownerId: Int, albumId: Int): Single<List<Video>>
    fun getById(
        accountId: Int,
        ownerId: Int,
        videoId: Int,
        accessKey: String?,
        cache: Boolean
    ): Single<Video>

    fun addToMy(accountId: Int, targetOwnerId: Int, videoOwnerId: Int, videoId: Int): Completable
    fun likeOrDislike(
        accountId: Int,
        ownerId: Int,
        videoId: Int,
        accessKey: String?,
        like: Boolean
    ): Single<Pair<Int, Boolean>>

    fun isLiked(accountId: Int, ownerId: Int, videoId: Int): Single<Boolean>
    fun checkAndAddLike(
        accountId: Int,
        ownerId: Int,
        videoId: Int,
        accessKey: String?
    ): Single<Int>

    fun getCachedAlbums(accountId: Int, ownerId: Int): Single<List<VideoAlbum>>
    fun getActualAlbums(
        accountId: Int,
        ownerId: Int,
        count: Int,
        offset: Int
    ): Single<List<VideoAlbum>>

    fun getAlbumsByVideo(
        accountId: Int,
        target_id: Int,
        owner_id: Int,
        video_id: Int
    ): Single<List<VideoAlbum>>

    fun search(
        accountId: Int,
        criteria: VideoSearchCriteria,
        count: Int,
        offset: Int
    ): Single<List<Video>>

    fun edit(
        accountId: Int,
        ownerId: Int,
        video_id: Int,
        name: String?,
        desc: String?
    ): Completable

    fun delete(accountId: Int, videoId: Int?, ownerId: Int?, targetId: Int?): Completable
}
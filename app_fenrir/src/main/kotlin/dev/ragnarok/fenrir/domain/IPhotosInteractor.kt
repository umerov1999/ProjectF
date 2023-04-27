package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.criteria.PhotoSearchCriteria
import dev.ragnarok.fenrir.model.AccessIdPair
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoTags
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IPhotosInteractor {
    operator fun get(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        count: Int,
        offset: Int,
        rev: Boolean
    ): Single<List<Photo>>

    fun getUsersPhoto(
        accountId: Long,
        ownerId: Long,
        extended: Int?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Single<List<Photo>>

    fun getAll(
        accountId: Long,
        ownerId: Long,
        extended: Int?,
        photo_sizes: Int?,
        offset: Int?,
        count: Int?
    ): Single<List<Photo>>

    fun search(
        accountId: Long,
        criteria: PhotoSearchCriteria,
        offset: Int?,
        count: Int?
    ): Single<List<Photo>>

    fun getAllCachedData(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        sortInvert: Boolean
    ): Single<List<Photo>>

    fun getAlbumById(accountId: Long, ownerId: Long, albumId: Int): Single<PhotoAlbum>
    fun getCachedAlbums(accountId: Long, ownerId: Long): Single<List<PhotoAlbum>>
    fun getActualAlbums(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Single<List<PhotoAlbum>>

    fun like(
        accountId: Long,
        ownerId: Long,
        photoId: Int,
        add: Boolean,
        accessKey: String?
    ): Single<Int>

    fun checkAndAddLike(
        accountId: Long,
        ownerId: Long,
        photoId: Int,
        accessKey: String?
    ): Single<Int>

    fun isLiked(accountId: Long, ownerId: Long, photoId: Int): Single<Boolean>
    fun copy(accountId: Long, ownerId: Long, photoId: Int, accessKey: String?): Single<Int>
    fun removedAlbum(accountId: Long, ownerId: Long, albumId: Int): Completable
    fun deletePhoto(accountId: Long, ownerId: Long, photoId: Int): Completable
    fun restorePhoto(accountId: Long, ownerId: Long, photoId: Int): Completable
    fun getPhotosByIds(accountId: Long, ids: Collection<AccessIdPair>): Single<List<Photo>>
    fun getTags(
        accountId: Long,
        ownerId: Long?,
        photo_id: Int?,
        access_key: String?
    ): Single<List<PhotoTags>>

    fun getAllComments(
        accountId: Long,
        ownerId: Long,
        album_id: Int?,
        offset: Int,
        count: Int
    ): Single<List<Comment>>
}
package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.api.model.VKApiPhotoTags
import dev.ragnarok.fenrir.fragment.search.criteria.PhotoSearchCriteria
import dev.ragnarok.fenrir.model.AccessIdPair
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IPhotosInteractor {
    operator fun get(
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        count: Int,
        offset: Int,
        rev: Boolean
    ): Single<List<Photo>>

    fun getUsersPhoto(
        accountId: Int,
        ownerId: Int,
        extended: Int?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Single<List<Photo>>

    fun getAll(
        accountId: Int,
        ownerId: Int,
        extended: Int?,
        photo_sizes: Int?,
        offset: Int?,
        count: Int?
    ): Single<List<Photo>>

    fun search(
        accountId: Int,
        criteria: PhotoSearchCriteria,
        offset: Int?,
        count: Int?
    ): Single<List<Photo>>

    fun getAllCachedData(
        accountId: Int,
        ownerId: Int,
        albumId: Int,
        sortInvert: Boolean
    ): Single<List<Photo>>

    fun getAlbumById(accountId: Int, ownerId: Int, albumId: Int): Single<PhotoAlbum>
    fun getCachedAlbums(accountId: Int, ownerId: Int): Single<List<PhotoAlbum>>
    fun getActualAlbums(
        accountId: Int,
        ownerId: Int,
        count: Int,
        offset: Int
    ): Single<List<PhotoAlbum>>

    fun like(
        accountId: Int,
        ownerId: Int,
        photoId: Int,
        add: Boolean,
        accessKey: String?
    ): Single<Int>

    fun checkAndAddLike(
        accountId: Int,
        ownerId: Int,
        photoId: Int,
        accessKey: String?
    ): Single<Int>

    fun isLiked(accountId: Int, ownerId: Int, photoId: Int): Single<Boolean>
    fun copy(accountId: Int, ownerId: Int, photoId: Int, accessKey: String?): Single<Int>
    fun removedAlbum(accountId: Int, ownerId: Int, albumId: Int): Completable
    fun deletePhoto(accountId: Int, ownerId: Int, photoId: Int): Completable
    fun restorePhoto(accountId: Int, ownerId: Int, photoId: Int): Completable
    fun getPhotosByIds(accountId: Int, ids: Collection<AccessIdPair>): Single<List<Photo>>
    fun getTags(
        accountId: Int,
        ownerId: Int?,
        photo_id: Int?,
        access_key: String?
    ): Single<List<VKApiPhotoTags>>

    fun getAllComments(
        accountId: Int,
        ownerId: Int,
        album_id: Int?,
        offset: Int,
        count: Int
    ): Single<List<Comment>>
}
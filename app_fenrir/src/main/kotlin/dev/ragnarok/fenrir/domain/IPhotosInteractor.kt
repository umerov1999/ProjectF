package dev.ragnarok.fenrir.domain

import dev.ragnarok.fenrir.fragment.search.criteria.PhotoSearchCriteria
import dev.ragnarok.fenrir.model.AccessIdPairModel
import dev.ragnarok.fenrir.model.Comment
import dev.ragnarok.fenrir.model.Photo
import dev.ragnarok.fenrir.model.PhotoAlbum
import dev.ragnarok.fenrir.model.PhotoTags
import kotlinx.coroutines.flow.Flow

interface IPhotosInteractor {
    operator fun get(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        count: Int,
        offset: Int,
        rev: Boolean
    ): Flow<List<Photo>>

    fun getUsersPhoto(
        accountId: Long,
        ownerId: Long,
        extended: Int?,
        sort: Int?,
        offset: Int?,
        count: Int?
    ): Flow<List<Photo>>

    fun getAll(
        accountId: Long,
        ownerId: Long,
        extended: Int?,
        photo_sizes: Int?,
        offset: Int?,
        count: Int?
    ): Flow<List<Photo>>

    fun search(
        accountId: Long,
        criteria: PhotoSearchCriteria,
        offset: Int?,
        count: Int?
    ): Flow<List<Photo>>

    fun getAllCachedData(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        sortInvert: Boolean
    ): Flow<List<Photo>>

    fun getAlbumById(accountId: Long, ownerId: Long, albumId: Int): Flow<PhotoAlbum>
    fun getCachedAlbums(accountId: Long, ownerId: Long): Flow<List<PhotoAlbum>>
    fun getActualAlbums(
        accountId: Long,
        ownerId: Long,
        count: Int,
        offset: Int
    ): Flow<List<PhotoAlbum>>

    fun like(
        accountId: Long,
        ownerId: Long,
        photoId: Int,
        add: Boolean,
        accessKey: String?
    ): Flow<Int>

    fun checkAndAddLike(
        accountId: Long,
        ownerId: Long,
        photoId: Int,
        accessKey: String?
    ): Flow<Int>

    fun isLiked(accountId: Long, ownerId: Long, photoId: Int): Flow<Boolean>
    fun copy(accountId: Long, ownerId: Long, photoId: Int, accessKey: String?): Flow<Int>
    fun removedAlbum(accountId: Long, ownerId: Long, albumId: Int): Flow<Boolean>
    fun deletePhoto(accountId: Long, ownerId: Long, photoId: Int): Flow<Boolean>
    fun restorePhoto(accountId: Long, ownerId: Long, photoId: Int): Flow<Boolean>
    fun getPhotosByIds(accountId: Long, ids: Collection<AccessIdPairModel>): Flow<List<Photo>>
    fun getTags(
        accountId: Long,
        ownerId: Long?,
        photo_id: Int?,
        access_key: String?
    ): Flow<List<PhotoTags>>

    fun getAllComments(
        accountId: Long,
        ownerId: Long,
        album_id: Int?,
        offset: Int,
        count: Int
    ): Flow<List<Comment>>
}
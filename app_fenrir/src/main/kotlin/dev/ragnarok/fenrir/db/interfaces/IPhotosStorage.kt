package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.PhotoPatch
import dev.ragnarok.fenrir.db.model.entity.PhotoDboEntity
import dev.ragnarok.fenrir.model.criteria.PhotoCriteria
import kotlinx.coroutines.flow.Flow

interface IPhotosStorage : IStorage {
    fun insertPhotosRx(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        photos: List<PhotoDboEntity>,
        clearBefore: Boolean
    ): Flow<Boolean>

    fun findPhotosByCriteriaRx(criteria: PhotoCriteria): Flow<List<PhotoDboEntity>>
    fun applyPatch(accountId: Long, ownerId: Long, photoId: Int, patch: PhotoPatch): Flow<Boolean>

    fun insertPhotosExtendedRx(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        photos: List<PhotoDboEntity>,
        clearBefore: Boolean
    ): Flow<Boolean>

    fun findPhotosExtendedByCriteriaRx(criteria: PhotoCriteria): Flow<List<PhotoDboEntity>>
}
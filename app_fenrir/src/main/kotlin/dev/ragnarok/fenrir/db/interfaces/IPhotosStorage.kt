package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.PhotoPatch
import dev.ragnarok.fenrir.db.model.entity.PhotoDboEntity
import dev.ragnarok.fenrir.model.criteria.PhotoCriteria
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IPhotosStorage : IStorage {
    fun insertPhotosRx(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        photos: List<PhotoDboEntity>,
        clearBefore: Boolean
    ): Completable

    fun findPhotosByCriteriaRx(criteria: PhotoCriteria): Single<List<PhotoDboEntity>>
    fun applyPatch(accountId: Long, ownerId: Long, photoId: Int, patch: PhotoPatch): Completable

    fun insertPhotosExtendedRx(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        photos: List<PhotoDboEntity>,
        clearBefore: Boolean
    ): Completable

    fun findPhotosExtendedByCriteriaRx(criteria: PhotoCriteria): Single<List<PhotoDboEntity>>
}
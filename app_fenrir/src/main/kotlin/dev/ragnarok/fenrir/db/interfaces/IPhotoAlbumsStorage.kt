package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.entity.PhotoAlbumDboEntity
import dev.ragnarok.fenrir.model.criteria.PhotoAlbumsCriteria
import dev.ragnarok.fenrir.util.Optional
import kotlinx.coroutines.flow.Flow

interface IPhotoAlbumsStorage : IStorage {
    @CheckResult
    fun findAlbumById(
        accountId: Long,
        ownerId: Long,
        albumId: Int
    ): Flow<Optional<PhotoAlbumDboEntity>>

    @CheckResult
    fun findAlbumsByCriteria(criteria: PhotoAlbumsCriteria): Flow<List<PhotoAlbumDboEntity>>

    @CheckResult
    fun store(
        accountId: Long,
        ownerId: Long,
        albums: List<PhotoAlbumDboEntity>,
        clearBefore: Boolean
    ): Flow<Boolean>

    @CheckResult
    fun removeAlbumById(accountId: Long, ownerId: Long, albumId: Int): Flow<Boolean>
}
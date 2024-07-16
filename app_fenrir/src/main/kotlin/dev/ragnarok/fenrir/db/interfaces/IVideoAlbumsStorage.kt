package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.VideoAlbumDboEntity
import dev.ragnarok.fenrir.model.VideoAlbumCriteria
import kotlinx.coroutines.flow.Flow

interface IVideoAlbumsStorage : IStorage {
    fun findByCriteria(criteria: VideoAlbumCriteria): Flow<List<VideoAlbumDboEntity>>
    fun insertData(
        accountId: Long,
        ownerId: Long,
        data: List<VideoAlbumDboEntity>,
        invalidateBefore: Boolean
    ): Flow<Boolean>
}
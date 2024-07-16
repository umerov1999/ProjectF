package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.entity.VideoDboEntity
import dev.ragnarok.fenrir.model.VideoCriteria
import kotlinx.coroutines.flow.Flow

interface IVideoStorage : IStorage {
    @CheckResult
    fun findByCriteria(criteria: VideoCriteria): Flow<List<VideoDboEntity>>

    @CheckResult
    fun insertData(
        accountId: Long,
        ownerId: Long,
        albumId: Int,
        videos: List<VideoDboEntity>,
        invalidateBefore: Boolean
    ): Flow<Boolean>
}
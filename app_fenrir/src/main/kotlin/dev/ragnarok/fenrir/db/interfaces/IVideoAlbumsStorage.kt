package dev.ragnarok.fenrir.db.interfaces

import dev.ragnarok.fenrir.db.model.entity.VideoAlbumDboEntity
import dev.ragnarok.fenrir.model.VideoAlbumCriteria
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IVideoAlbumsStorage : IStorage {
    fun findByCriteria(criteria: VideoAlbumCriteria): Single<List<VideoAlbumDboEntity>>
    fun insertData(
        accountId: Int,
        ownerId: Int,
        data: List<VideoAlbumDboEntity>,
        invalidateBefore: Boolean
    ): Completable
}
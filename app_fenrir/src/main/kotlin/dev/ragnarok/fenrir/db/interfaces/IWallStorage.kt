package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.PostPatch
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.util.Optional
import kotlinx.coroutines.flow.Flow

interface IWallStorage : IStorage {
    @CheckResult
    fun storeWallEntities(
        accountId: Long, posts: List<PostDboEntity>,
        owners: OwnerEntities?,
        clearWall: IClearWallTask?
    ): Flow<IntArray>

    @CheckResult
    fun replacePost(accountId: Long, post: PostDboEntity): Flow<Int>

    @CheckResult
    fun getEditingPost(
        accountId: Long,
        ownerId: Long,
        @EditingPostType type: Int,
        includeAttachment: Boolean
    ): Flow<PostDboEntity>

    @CheckResult
    fun deletePost(accountId: Long, dbid: Int): Flow<Boolean>

    @CheckResult
    fun findPostById(accountId: Long, dbid: Int): Flow<Optional<PostDboEntity>>

    @CheckResult
    fun findPostById(
        accountId: Long,
        ownerId: Long,
        vkpostId: Int,
        includeAttachment: Boolean
    ): Flow<Optional<PostDboEntity>>

    fun findDbosByCriteria(criteria: WallCriteria): Flow<List<PostDboEntity>>

    @CheckResult
    fun update(accountId: Long, ownerId: Long, postId: Int, update: PostPatch): Flow<Boolean>

    /**
     * Уведомить хранилище, что пост более не существует
     */
    fun invalidatePost(accountId: Long, postVkid: Int, postOwnerId: Long): Flow<Boolean>
    interface IClearWallTask {
        val ownerId: Long
    }
}
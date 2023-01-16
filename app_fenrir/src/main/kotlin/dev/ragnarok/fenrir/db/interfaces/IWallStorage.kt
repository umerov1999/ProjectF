package dev.ragnarok.fenrir.db.interfaces

import androidx.annotation.CheckResult
import dev.ragnarok.fenrir.db.model.PostPatch
import dev.ragnarok.fenrir.db.model.entity.OwnerEntities
import dev.ragnarok.fenrir.db.model.entity.PostDboEntity
import dev.ragnarok.fenrir.model.EditingPostType
import dev.ragnarok.fenrir.model.criteria.WallCriteria
import dev.ragnarok.fenrir.util.Optional
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single

interface IWallStorage : IStorage {
    @CheckResult
    fun storeWallEntities(
        accountId: Long, posts: List<PostDboEntity>,
        owners: OwnerEntities?,
        clearWall: IClearWallTask?
    ): Single<IntArray>

    @CheckResult
    fun replacePost(accountId: Long, post: PostDboEntity): Single<Int>

    @CheckResult
    fun getEditingPost(
        accountId: Long,
        ownerId: Long,
        @EditingPostType type: Int,
        includeAttachment: Boolean
    ): Single<PostDboEntity>

    @CheckResult
    fun deletePost(accountId: Long, dbid: Int): Completable

    @CheckResult
    fun findPostById(accountId: Long, dbid: Int): Single<Optional<PostDboEntity>>

    @CheckResult
    fun findPostById(
        accountId: Long,
        ownerId: Long,
        vkpostId: Int,
        includeAttachment: Boolean
    ): Single<Optional<PostDboEntity>>

    fun findDbosByCriteria(criteria: WallCriteria): Single<List<PostDboEntity>>

    @CheckResult
    fun update(accountId: Long, ownerId: Long, postId: Int, update: PostPatch): Completable

    /**
     * Уведомить хранилище, что пост более не существует
     */
    fun invalidatePost(accountId: Long, postVkid: Int, postOwnerId: Long): Completable
    interface IClearWallTask {
        val ownerId: Long
    }
}
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
        accountId: Int, posts: List<PostDboEntity>,
        owners: OwnerEntities?,
        clearWall: IClearWallTask?
    ): Single<IntArray>

    @CheckResult
    fun replacePost(accountId: Int, post: PostDboEntity): Single<Int>

    @CheckResult
    fun getEditingPost(
        accountId: Int,
        ownerId: Int,
        @EditingPostType type: Int,
        includeAttachment: Boolean
    ): Single<PostDboEntity>

    @CheckResult
    fun deletePost(accountId: Int, dbid: Int): Completable

    @CheckResult
    fun findPostById(accountId: Int, dbid: Int): Single<Optional<PostDboEntity>>

    @CheckResult
    fun findPostById(
        accountId: Int,
        ownerId: Int,
        vkpostId: Int,
        includeAttachment: Boolean
    ): Single<Optional<PostDboEntity>>

    fun findDbosByCriteria(criteria: WallCriteria): Single<List<PostDboEntity>>

    @CheckResult
    fun update(accountId: Int, ownerId: Int, postId: Int, update: PostPatch): Completable

    /**
     * Уведомить хранилище, что пост более не существует
     */
    fun invalidatePost(accountId: Int, postVkid: Int, postOwnerId: Int): Completable
    interface IClearWallTask {
        val ownerId: Int
    }
}